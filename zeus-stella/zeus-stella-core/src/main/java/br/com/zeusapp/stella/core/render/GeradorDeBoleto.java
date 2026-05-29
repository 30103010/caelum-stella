package br.com.zeusapp.stella.core.render;

import br.com.zeusapp.stella.core.exception.BoletoRenderException;
import br.com.zeusapp.stella.core.model.BoletoReportModel;
import br.com.zeusapp.stella.dto.BoletoDTO;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Nota: parâmetros de imagem (BARCODE_IMAGE, QRCODE_IMAGE) são passados como
// ByteArrayInputStream — tipo suportado nativamente pelo JasperReports image element.

/**
 * Gerador de PDFs de boleto para o Zeus Stella.
 *
 * Fluxo padrão:
 *   1. BancoConnector.registrar(boleto)   → preenche boleto.linhaDigitavel, boleto.codigoBarras, boleto.emvPix
 *   2. GeradorDeBoleto.geraPDF(boleto)    → gera PDF pronto para exibição/download
 *
 * Lógica do template:
 *   - Se boleto.emvPix != null  → renderiza QR Code no canto inferior direito da ficha
 *   - Se boleto.codigoBarras != null → renderiza barcode ITF via ZXing
 *   - TipoLayout "CARNE"        → usa template boleto-zeus-carne.jrxml
 *   - Outros                    → usa template boleto-zeus.jrxml (padrão)
 *
 * Os templates .jrxml são compilados em runtime na primeira chamada de cada layout
 * e reutilizados via cache (thread-safe). Para produção, use o jasperreports-maven-plugin
 * para pré-compilar os templates .jrxml → .jasper, eliminando o overhead de compilação.
 */
public class GeradorDeBoleto {

    // Nomes dos parâmetros JasperReports
    public static final String PARAM_BARCODE_IMAGE = "BARCODE_IMAGE";
    public static final String PARAM_QRCODE_IMAGE  = "QRCODE_IMAGE";
    public static final String PARAM_EXIBIR_PIX    = "EXIBIR_PIX";

    private static final String TEMPLATE_PADRAO =
            "/br/com/zeusapp/stella/core/templates/boleto-zeus.jrxml";
    private static final String TEMPLATE_CARNE  =
            "/br/com/zeusapp/stella/core/templates/boleto-zeus-carne.jrxml";

    // Cache de templates compilados: chave = caminho do .jrxml
    private static final Map<String, JasperReport> TEMPLATE_CACHE = new ConcurrentHashMap<>();

    private final GeradorDeQRCode qrCodeGen;

    public GeradorDeBoleto() {
        this.qrCodeGen = new GeradorDeQRCode();
    }

    /** Construtor para testes ou injeção de dependência. */
    public GeradorDeBoleto(GeradorDeQRCode qrCodeGen) {
        this.qrCodeGen = qrCodeGen;
    }

    // -----------------------------------------------------------------------
    // API pública
    // -----------------------------------------------------------------------

    /**
     * Gera o PDF do boleto usando o template padrão ou carnê conforme tipoLayout.
     *
     * @param boleto DTO completo (linhaDigitavel e codigoBarras preenchidos pelo connector)
     * @return bytes do PDF gerado
     */
    public byte[] geraPDF(BoletoDTO boleto) {
        String templatePath = resolverTemplatePath(boleto.getTipoLayout());
        return geraPDFComTemplate(boleto, obterTemplate(templatePath));
    }

    /**
     * Gera o PDF usando um template customizado fornecido como InputStream (.jrxml).
     * O template é compilado a cada chamada — use apenas para desenvolvimento/teste.
     *
     * @param boleto   DTO completo
     * @param jrxmlIs  InputStream do arquivo .jrxml personalizado
     * @return bytes do PDF gerado
     */
    public byte[] geraPDFComTemplateCustom(BoletoDTO boleto, InputStream jrxmlIs) {
        try {
            JasperReport report = JasperCompileManager.compileReport(jrxmlIs);
            return geraPDFComTemplate(boleto, report);
        } catch (JRException e) {
            throw new BoletoRenderException("Falha ao compilar template customizado", e);
        }
    }

    /**
     * Gera o PDF e retorna como InputStream.
     *
     * @param boleto DTO completo
     * @return InputStream do PDF
     */
    public InputStream geraPDFStream(BoletoDTO boleto) {
        return new ByteArrayInputStream(geraPDF(boleto));
    }

    // -----------------------------------------------------------------------
    // Implementação interna
    // -----------------------------------------------------------------------

    private byte[] geraPDFComTemplate(BoletoDTO boleto, JasperReport report) {
        try {
            BoletoReportModel model = BoletoReportModel.from(boleto);
            Map<String, Object> params = montarParametros(boleto);

            JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(List.of(model));
            JasperPrint print = JasperFillManager.fillReport(report, params, ds);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            JasperExportManager.exportReportToPdfStream(print, out);
            return out.toByteArray();

        } catch (JRException e) {
            throw new BoletoRenderException("Falha ao gerar PDF do boleto", e);
        }
    }

    private Map<String, Object> montarParametros(BoletoDTO boleto) {
        Map<String, Object> params = new HashMap<>();
        params.put(JRParameter.REPORT_LOCALE, new Locale("pt", "BR"));

        // Código de barras ITF (FEBRABAN 44 dígitos) — passado como InputStream para JasperReports
        if (boleto.getCodigoBarras() != null && !boleto.getCodigoBarras().isBlank()) {
            try {
                byte[] barcodeBytes = qrCodeGen.gerarCodigoBarrasITF(boleto.getCodigoBarras(), 800, 70);
                params.put(PARAM_BARCODE_IMAGE, new ByteArrayInputStream(barcodeBytes));
            } catch (BoletoRenderException e) {
                // Barcode inválido não deve impedir geração do boleto — parâmetro null = em branco
                params.put(PARAM_BARCODE_IMAGE, null);
            }
        } else {
            params.put(PARAM_BARCODE_IMAGE, null);
        }

        // QR Code PIX — passado como InputStream para JasperReports
        if (boleto.getEmvPix() != null && !boleto.getEmvPix().isBlank()) {
            byte[] qrBytes = qrCodeGen.gerarQRCode(boleto.getEmvPix(), 200, 200);
            params.put(PARAM_QRCODE_IMAGE, new ByteArrayInputStream(qrBytes));
            params.put(PARAM_EXIBIR_PIX, Boolean.TRUE);
        } else {
            params.put(PARAM_QRCODE_IMAGE, null);
            params.put(PARAM_EXIBIR_PIX, Boolean.FALSE);
        }

        return params;
    }

    private JasperReport obterTemplate(String templatePath) {
        return TEMPLATE_CACHE.computeIfAbsent(templatePath, path -> {
            InputStream jrxml = GeradorDeBoleto.class.getResourceAsStream(path);
            if (jrxml == null) {
                throw new BoletoRenderException("Template não encontrado no classpath: " + path);
            }
            try {
                return JasperCompileManager.compileReport(jrxml);
            } catch (JRException e) {
                throw new BoletoRenderException("Falha ao compilar template: " + path, e);
            }
        });
    }

    private static String resolverTemplatePath(String tipoLayout) {
        if ("CARNE".equalsIgnoreCase(tipoLayout)) {
            return TEMPLATE_CARNE;
        }
        return TEMPLATE_PADRAO;
    }
}
