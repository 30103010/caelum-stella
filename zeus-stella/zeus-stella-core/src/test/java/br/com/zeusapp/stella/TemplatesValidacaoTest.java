package br.com.zeusapp.stella;

import br.com.zeusapp.stella.core.render.GeradorDeBoleto;
import br.com.zeusapp.stella.dto.BoletoDTO;
import org.junit.jupiter.api.*;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Valida a geração de PDFs pelos templates JasperReports do zeus-stella-core.
 *
 * Saída: e:\temp\teste_sem_sacador.pdf  (boleto-zeus.jrxml — adaptação de boleto-sem-sacador-avalista)
 *        e:\temp\teste_carne.pdf        (boleto-zeus-carne.jrxml — 3 parcelas concatenadas)
 *        e:\temp\teste_padrao.pdf       (boleto-zeus.jrxml — com QR Code PIX ativo)
 *
 * Nota técnica: os templates LEGADOS (boleto-sem-sacador-avalista.jrxml, boleto-carne.jrxml,
 * boleto-padrao.jrxml) usam o componente <jr:barbecue> e a API antiga do Boleto.java —
 * ambos ausentes em zeus-stella-core. Os templates testados aqui são suas adaptações
 * produção-prontas que usam $P{BARCODE_IMAGE} (ZXing) e BoletoReportModel (flat API).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TemplatesValidacaoTest {

    // ── Configuração ──────────────────────────────────────────────────────────

    private static final String TEMP = "e:/temp";

    /**
     * Código de barras FEBRABAN 44 dígitos (valor fictício para testes).
     * ITF requer par de dígitos — 44 é par. ✓
     */
    private static final String CODIGO_BARRAS =
            "00190000000000000001000000000000000000000001";

    private static final String LINHA_DIGITAVEL =
            "00190.00001 00000.000000 00000.000000 1 00000000060000";

    private final GeradorDeBoleto gerador = new GeradorDeBoleto();

    @BeforeAll
    void prepararDiretorio() throws Exception {
        Files.createDirectories(Path.of(TEMP));
        System.out.println("[setup] PDFs serão salvos em: " + Path.of(TEMP).toAbsolutePath());
    }

    // ── Fábrica de dados mockados ─────────────────────────────────────────────

    private BoletoDTO criarBoleto(String tipoLayout,
                                  LocalDate vencimento,
                                  Integer parcela,
                                  Integer totalParcelas,
                                  String emvPix) {
        return BoletoDTO.builder()
                // ── Beneficiário ──
                .nomeBeneficiario("TOLEDO LOC.E ADM.DE BENS LTDA")
                .cnpjCpfBeneficiario("27.276.390/0001-83")
                .enderecoBeneficiario("Rua das Acácias, 123 — Centro — São Paulo/SP — CEP 01010-010")
                .agencia("0268")
                .digitoAgencia("2")
                .codigoBeneficiario("49333")
                .digitoCodigoBeneficiario("3")
                .carteira("17")
                .convenio("0019002")
                // ── Pagador ──
                .nomePagador("CLIENTE TESTE")
                .cpfCnpjPagador("111.222.333-44")
                .enderecoPagador("Av. Paulista, 1000 — Bela Vista — CEP 01310-100 — São Paulo-SP")
                // ── Título ──
                .valorDocumento(new BigDecimal("600.00"))
                .vencimento(vencimento)
                .dataDocumento(LocalDate.now())
                .dataProcessamento(LocalDate.now())
                .numeroDocumento("000001")
                .nossoNumero("000000000001")
                .especie("DM")
                .aceite("N")
                .usoBanco("")
                .localPagamento("Pagável em qualquer banco até o vencimento.")
                .instrucoes(List.of(
                        "Cobrar multa de 2% após vencimento",
                        "Cobrar juros de 0,033% ao dia"
                ))
                // ── Parcela (carnê) ──
                .parcela(parcela)
                .totalParcelas(totalParcelas)
                // ── PIX ──
                .emvPix(emvPix)
                // ── Banco / Layout ──
                .codigoBanco("001")
                .tipoLayout(tipoLayout)
                // ── Barras (preenchidos após registro no banco) ──
                .linhaDigitavel(LINHA_DIGITAVEL)
                .codigoBarras(CODIGO_BARRAS)
                .build();
    }

    // ── Teste 1: boleto-zeus.jrxml → teste_sem_sacador.pdf ───────────────────

    @Test
    @Order(1)
    @DisplayName("Template PADRAO (boleto-zeus.jrxml) → teste_sem_sacador.pdf")
    void testeSemSacadorAvalista() throws Exception {
        System.out.println("\n── Teste 1: boleto-zeus.jrxml (adaptação de boleto-sem-sacador-avalista) ──");

        LocalDate venc = LocalDate.now().plusDays(30);
        BoletoDTO boleto = criarBoleto("PADRAO", venc, null, null, null);

        byte[] pdf = gerador.geraPDF(boleto);

        Path saida = Path.of(TEMP, "teste_sem_sacador.pdf");
        Files.write(saida, pdf);

        assertAll(
                () -> assertTrue(saida.toFile().exists(),   "Arquivo não existe"),
                () -> assertTrue(saida.toFile().length() > 5_000,
                        "PDF muito pequeno (" + saida.toFile().length() + " bytes) — possível erro de render")
        );
        System.out.printf("   [OK] %s — %,d bytes%n", saida.toAbsolutePath(), saida.toFile().length());
    }

    // ── Teste 2: boleto-zeus-carne.jrxml → teste_carne.pdf (3 parcelas) ──────

    @Test
    @Order(2)
    @DisplayName("Template CARNE (boleto-zeus-carne.jrxml) → teste_carne.pdf (3 parcelas)")
    void testeCarne3Parcelas() throws Exception {
        System.out.println("\n── Teste 2: boleto-zeus-carne.jrxml (adaptação de boleto-carne) — 3 parcelas ──");

        LocalDate venc1 = LocalDate.now().plusDays(30);
        LocalDate venc2 = venc1.plusMonths(1);
        LocalDate venc3 = venc1.plusMonths(2);

        byte[] pdf1 = gerador.geraPDF(criarBoleto("CARNE", venc1, 1, 3, null));
        byte[] pdf2 = gerador.geraPDF(criarBoleto("CARNE", venc2, 2, 3, null));
        byte[] pdf3 = gerador.geraPDF(criarBoleto("CARNE", venc3, 3, 3, null));

        System.out.printf("   Parcela 1: %,d bytes%n", pdf1.length);
        System.out.printf("   Parcela 2: %,d bytes%n", pdf2.length);
        System.out.printf("   Parcela 3: %,d bytes%n", pdf3.length);

        // Concatena 3 páginas em 1 PDF usando iText (transitive dep de JasperReports)
        byte[] carnePdf = concatenarPdfs(pdf1, pdf2, pdf3);

        Path saida = Path.of(TEMP, "teste_carne.pdf");
        Files.write(saida, carnePdf);

        assertAll(
                () -> assertTrue(saida.toFile().exists(),   "Arquivo não existe"),
                () -> assertTrue(saida.toFile().length() > 10_000,
                        "PDF do carnê muito pequeno (" + saida.toFile().length() + " bytes)")
        );
        System.out.printf("   [OK] %s — %,d bytes (3 páginas)%n", saida.toAbsolutePath(), saida.toFile().length());
    }

    // ── Teste 3: boleto-zeus.jrxml + PIX QR Code → teste_padrao.pdf ──────────

    @Test
    @Order(3)
    @DisplayName("Template PADRAO + PIX QR Code (boleto-zeus.jrxml) → teste_padrao.pdf")
    void testePadraoComPixQrCode() throws Exception {
        System.out.println("\n── Teste 3: boleto-zeus.jrxml com PIX QR Code ativo ──");

        // String EMV Pix válida de exemplo (padrão BACEN)
        // Em produção este valor vem de BancoDoBrasilConnector.registrar().getEmvPix()
        String emvPix = "00020126580014BR.GOV.BCB.PIX"
                + "013600000000-0000-0000-0000-000000000000"
                + "5204000053039865802BR"
                + "5923TOLEDO LOC ADM DE BENS"
                + "6009SAO PAULO"
                + "62070503***"
                + "63041234";

        LocalDate venc = LocalDate.now().plusDays(30);
        BoletoDTO boleto = criarBoleto("PADRAO", venc, null, null, emvPix);

        // PIX txid (simulado)
        boleto.setTxid("E27276390202501011200000000000001");

        byte[] pdf = gerador.geraPDF(boleto);

        Path saida = Path.of(TEMP, "teste_padrao.pdf");
        Files.write(saida, pdf);

        assertAll(
                () -> assertTrue(saida.toFile().exists(),   "Arquivo não existe"),
                () -> assertTrue(saida.toFile().length() > 5_000,
                        "PDF muito pequeno (" + saida.toFile().length() + " bytes) — QR Code pode não ter sido gerado")
        );
        System.out.printf("   [OK] %s — %,d bytes (com QR Code PIX)%n", saida.toAbsolutePath(), saida.toFile().length());
    }

    // ── Utilitário: concatenação de PDFs ──────────────────────────────────────

    /**
     * Concatena múltiplos PDFs em um único arquivo usando iText 2.1.7
     * (dependência transitiva de net.sf.jasperreports:jasperreports).
     */
    private byte[] concatenarPdfs(byte[]... pdfs) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            com.lowagie.text.Document doc = new com.lowagie.text.Document();
            com.lowagie.text.pdf.PdfCopy copy = new com.lowagie.text.pdf.PdfCopy(doc, baos);
            doc.open();
            for (byte[] pdfBytes : pdfs) {
                com.lowagie.text.pdf.PdfReader reader =
                        new com.lowagie.text.pdf.PdfReader(pdfBytes);
                for (int pg = 1; pg <= reader.getNumberOfPages(); pg++) {
                    copy.addPage(copy.getImportedPage(reader, pg));
                }
                reader.close();
            }
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Falha ao concatenar PDFs: " + e.getMessage(), e);
        }
    }
}
