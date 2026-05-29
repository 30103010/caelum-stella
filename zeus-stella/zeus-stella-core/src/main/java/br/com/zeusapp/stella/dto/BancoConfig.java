package br.com.zeusapp.stella.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BancoConfig {

    /** Código do banco: "001"=BB, "237"=Bradesco, "341"=Itaú, "748"=Sicoob */
    private String codigoBanco;

    // OAuth2 (BB, Sicoob, Itaú API)
    private String clientId;
    private String clientSecret;

    // mTLS — certificado .pfx (Bradesco, Itaú legado)
    private String certificadoPfxPath;
    private String certificadoSenha;

    /** "SANDBOX" ou "PRODUCAO" */
    private String ambiente;

    // Dados bancários do beneficiário
    private String convenio;
    private String carteira;
    private String agencia;
    private String conta;

    /**
     * Propriedades extras específicas de cada banco.
     * Mantém o core agnóstico: o connector lê as chaves que precisa.
     *
     * Exemplos:
     *   BB       → extras.put("developerAppKey", "abc123")   // header gw-dev-app-key
     *   Bradesco → extras.put("merchantId", "12345678")
     *   Sicoob   → extras.put("chaveAcesso", "xxx")
     */
    @Builder.Default
    private Map<String, String> extras = new HashMap<>();
}
