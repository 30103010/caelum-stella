package br.com.zeusapp.stella.connector.impl;

import br.com.zeusapp.stella.connector.BancoConnector;
import br.com.zeusapp.stella.connector.exception.AutenticacaoException;
import br.com.zeusapp.stella.connector.exception.ConsultaBoletoException;
import br.com.zeusapp.stella.connector.exception.RegistroBoletoException;
import br.com.zeusapp.stella.dto.BancoConfig;
import br.com.zeusapp.stella.dto.BoletoConsultaDTO;
import br.com.zeusapp.stella.dto.BoletoDTO;
import br.com.zeusapp.stella.dto.BoletoRegistradoDTO;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;

/**
 * Connector para a API de Cobrança do Banco do Brasil v2.
 *
 * Autenticação: OAuth2 client_credentials + header obrigatório gw-dev-app-key.
 * Docs: https://developers.bb.com.br/apis/cobrancas
 *
 * Configuração via BancoConfig.extras:
 *   config.getExtras().put("developerAppKey", "<sua-chave>")
 *
 * Instanciação recomendada no contexto Spring da aplicação consumidora:
 *
 *   @Bean
 *   public BancoConnector bbConnector(@Value("${bb.client-id}") String clientId, ...) {
 *       BancoConfig cfg = BancoConfig.builder()
 *           .codigoBanco("001")
 *           .clientId(clientId)
 *           .clientSecret(clientSecret)
 *           .ambiente("SANDBOX")
 *           .extras(Map.of("developerAppKey", developerAppKey))
 *           .build();
 *       return new BancoDoBrasilConnector(cfg);
 *   }
 */
public class BancoDoBrasilConnector implements BancoConnector {

    public static final String CODIGO_BANCO = "001";

    private static final String BASE_SANDBOX  = "https://sandbox.api.bb.com.br";
    private static final String BASE_PRODUCAO = "https://api.bb.com.br";
    private static final String PATH_TOKEN    = "/oauth/token";
    private static final String PATH_BOLETOS  = "/cobrancas/v2/boletos";

    private final BancoConfig config;
    private final WebClient   webClient;

    // Cache de token OAuth2 — thread-safe com double-checked locking
    private volatile String  tokenCache;
    private volatile Instant tokenExpiry = Instant.EPOCH;

    public BancoDoBrasilConnector(BancoConfig config) {
        this.config    = config;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // -------------------------------------------------------------------------
    // BancoConnector
    // -------------------------------------------------------------------------

    @Override
    public BoletoRegistradoDTO registrar(BoletoDTO boleto) {
        String token = obterToken();
        Map<String, Object> body = montarBodyRegistro(boleto);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = webClient.post()
                    .uri(PATH_BOLETOS)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header("gw-dev-app-key", developerAppKey())
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return mapearRegistro(resp);

        } catch (WebClientResponseException ex) {
            throw new RegistroBoletoException(CODIGO_BANCO,
                    "Erro ao registrar boleto BB: " + ex.getResponseBodyAsString(),
                    ex.getStatusCode().value(), ex);
        }
    }

    @Override
    public BoletoConsultaDTO consultar(String nossoNumero) {
        String token = obterToken();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = webClient.get()
                    .uri(uri -> uri
                            .path(PATH_BOLETOS + "/{nossoNumero}")
                            .queryParam("numeroConvenio", config.getConvenio())
                            .build(nossoNumero))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header("gw-dev-app-key", developerAppKey())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return mapearConsulta(resp);

        } catch (WebClientResponseException ex) {
            throw new ConsultaBoletoException(CODIGO_BANCO,
                    "Erro ao consultar boleto BB: " + ex.getResponseBodyAsString(),
                    ex.getStatusCode().value(), ex);
        }
    }

    @Override
    public void cancelar(String nossoNumero) {
        String token = obterToken();
        Map<String, Object> body = Map.of(
                "numeroConvenio",  config.getConvenio(),
                "descricaoBaixar", "BAIXA A PEDIDO DO BENEFICIARIO"
        );

        try {
            webClient.post()
                    .uri(PATH_BOLETOS + "/{nossoNumero}/baixar", nossoNumero)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header("gw-dev-app-key", developerAppKey())
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();

        } catch (WebClientResponseException ex) {
            throw new RegistroBoletoException(CODIGO_BANCO,
                    "Erro ao cancelar boleto BB: " + ex.getResponseBodyAsString(),
                    ex.getStatusCode().value(), ex);
        }
    }

    @Override
    public String codigoBanco() {
        return CODIGO_BANCO;
    }

    @Override
    public BancoConfig getConfig() {
        return config;
    }

    // -------------------------------------------------------------------------
    // OAuth2 — client_credentials com cache de token
    // -------------------------------------------------------------------------

    private String obterToken() {
        if (tokenCache != null && Instant.now().isBefore(tokenExpiry.minusSeconds(30))) {
            return tokenCache;
        }
        return renovarToken();
    }

    private synchronized String renovarToken() {
        // double-checked locking para evitar chamadas duplas sob concorrência
        if (tokenCache != null && Instant.now().isBefore(tokenExpiry.minusSeconds(30))) {
            return tokenCache;
        }

        String credenciais = Base64.getEncoder().encodeToString(
                (config.getClientId() + ":" + config.getClientSecret()).getBytes());

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = WebClient.create(baseUrl())
                    .post()
                    .uri(PATH_TOKEN)
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + credenciais)
                    .header("gw-dev-app-key", developerAppKey())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("grant_type", "client_credentials")
                            .with("scope", "cobrancas.boletos-requisicao cobrancas.boletos-info"))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            this.tokenCache  = (String) resp.get("access_token");
            int expiresIn    = ((Number) resp.get("expires_in")).intValue();
            this.tokenExpiry = Instant.now().plusSeconds(expiresIn);
            return this.tokenCache;

        } catch (WebClientResponseException ex) {
            throw new AutenticacaoException(CODIGO_BANCO,
                    "Falha OAuth2 BB: " + ex.getResponseBodyAsString(), ex);
        }
    }

    // -------------------------------------------------------------------------
    // Mapeamento request / response (formato API BB v2)
    // Ref: https://developers.bb.com.br/apis/cobrancas#tag/Boletos/operation/registrarBoleto
    // -------------------------------------------------------------------------

    private Map<String, Object> montarBodyRegistro(BoletoDTO boleto) {
        return Map.ofEntries(
                Map.entry("numeroConvenio",          config.getConvenio()),
                Map.entry("numeroCarteira",           Integer.parseInt(config.getCarteira())),
                Map.entry("numeroVariacaoCarteira",   35),
                Map.entry("codigoModalidade",         1),
                Map.entry("dataEmissao",              boleto.getDataDocumento().toString()),
                Map.entry("dataVencimento",           boleto.getVencimento().toString()),
                Map.entry("valorOriginal",            boleto.getValorDocumento()),
                Map.entry("codigoAceite",             "A"),
                Map.entry("codigoTipoTitulo",         2),
                Map.entry("indicadorPix",             boleto.getEmvPix() != null ? "S" : "N"),
                Map.entry("pagador", Map.of(
                        "tipoInscricao",   boleto.getCpfCnpjPagador().length() == 11 ? 1 : 2,
                        "numeroInscricao", boleto.getCpfCnpjPagador(),
                        "nome",            boleto.getNomePagador()
                ))
        );
    }

    @SuppressWarnings("unchecked")
    private BoletoRegistradoDTO mapearRegistro(Map<String, Object> resp) {
        Map<String, Object> qrCode = (Map<String, Object>) resp.get("qrCode");
        return BoletoRegistradoDTO.builder()
                .linhaDigitavel((String) resp.get("codigoLinhaDigitavel"))
                .codigoBarras((String) resp.get("codigoBarraNumerico"))
                .nossoNumeroFormatado(String.valueOf(resp.get("numero")))
                .emvPix(qrCode != null ? (String) qrCode.get("emv")  : null)
                .txid(qrCode  != null ? (String) qrCode.get("txId") : null)
                .dataRegistro(LocalDateTime.now())
                .build();
    }

    private BoletoConsultaDTO mapearConsulta(Map<String, Object> resp) {
        // TODO: mapear situação BB -> situação normalizada (A_RECEBER, LIQUIDADO, BAIXADO, VENCIDO)
        return BoletoConsultaDTO.builder()
                .nossoNumero(String.valueOf(resp.get("numero")))
                .linhaDigitavel((String) resp.get("codigoLinhaDigitavel"))
                .codigoBarras((String) resp.get("codigoBarraNumerico"))
                .situacao(String.valueOf(resp.get("codigoEstadoBoleto")))
                .dataConsulta(LocalDateTime.now())
                .build();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String developerAppKey() {
        return config.getExtras().getOrDefault("developerAppKey", "");
    }

    private String baseUrl() {
        return "SANDBOX".equalsIgnoreCase(config.getAmbiente()) ? BASE_SANDBOX : BASE_PRODUCAO;
    }
}
