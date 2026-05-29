package br.com.zeusapp.stella.core.model;

import br.com.zeusapp.stella.dto.BoletoDTO;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Modelo interno plano usado pelo JasperReports.
 *
 * Traduz o BoletoDTO (rico, tipado com LocalDate, etc.) para um JavaBean
 * com todos os campos que o template boleto-zeus.jrxml lê diretamente via
 * JRBeanCollectionDataSource — sem navegação aninhada de propriedades.
 */
public class BoletoReportModel {

    // -----------------------------------------------------------------------
    // Beneficiário
    // -----------------------------------------------------------------------
    private String nomeBeneficiario;
    private String documentoBeneficiario;
    private String enderecoBeneficiario;
    private String agenciaECodigoBeneficiario;
    private String nossoNumeroECodDocumento;
    private String digitoNossoNumero;
    private String carteira;
    private URL    logoBanco;
    private String numeroBancoComDigito;

    // -----------------------------------------------------------------------
    // Pagador
    // -----------------------------------------------------------------------
    private String nomePagador;
    private String documentoPagador;
    private String logradouroPagador;
    private String bairroPagador;
    private String cepPagador;
    private String cidadePagador;
    private String ufPagador;

    // -----------------------------------------------------------------------
    // Datas — java.util.Date para compatibilidade com JasperReports patterns
    // -----------------------------------------------------------------------
    private Date vencimento;
    private Date dataDocumento;
    private Date dataProcessamento;

    // -----------------------------------------------------------------------
    // Valores financeiros
    // -----------------------------------------------------------------------
    private BigDecimal valorBoleto;
    private BigDecimal valorCobrado;
    private BigDecimal valorDescontos;
    private BigDecimal valorDeducoes;
    private BigDecimal valorMulta;
    private BigDecimal valorAcrescimos;
    private BigDecimal quantidadeMoeda;
    private BigDecimal valorMoeda;

    // -----------------------------------------------------------------------
    // Metadados do título
    // -----------------------------------------------------------------------
    private String       especieDocumento;
    private String       especieMoeda;
    private Boolean      aceite;
    private String       localPagamento;
    private String       numeroDocumento;
    private List<String> instrucoes;

    // -----------------------------------------------------------------------
    // Barras
    // -----------------------------------------------------------------------
    private String linhaDigitavel;
    private String codigoDeBarras;

    // -----------------------------------------------------------------------
    // Carnê (null para boleto simples)
    // -----------------------------------------------------------------------
    private String parcela;
    private String totalParcelas;

    // -----------------------------------------------------------------------
    // Factory method
    // -----------------------------------------------------------------------

    public static BoletoReportModel from(BoletoDTO dto) {
        BoletoReportModel m = new BoletoReportModel();

        // Beneficiário
        m.nomeBeneficiario             = nvl(dto.getNomeBeneficiario());
        m.documentoBeneficiario        = nvl(dto.getCnpjCpfBeneficiario());
        m.enderecoBeneficiario         = nvl(dto.getEnderecoBeneficiario());
        m.agenciaECodigoBeneficiario   = formatarAgenciaConta(dto);
        m.nossoNumeroECodDocumento     = nvl(dto.getNossoNumero());
        m.digitoNossoNumero            = "";
        m.carteira                     = nvl(dto.getCarteira());
        m.logoBanco                    = resolverLogoBanco(dto.getCodigoBanco());
        m.numeroBancoComDigito         = nvl(dto.getCodigoBanco());

        // Pagador — endereço armazenado como string única em BoletoDTO
        m.nomePagador                  = nvl(dto.getNomePagador());
        m.documentoPagador             = nvl(dto.getCpfCnpjPagador());
        m.logradouroPagador            = nvl(dto.getEnderecoPagador());
        m.bairroPagador                = "";
        m.cepPagador                   = "";
        m.cidadePagador                = "";
        m.ufPagador                    = "";

        // Datas
        m.vencimento                   = toDate(dto.getVencimento());
        m.dataDocumento                = toDate(dto.getDataDocumento());
        m.dataProcessamento            = toDate(nvlDate(dto.getDataProcessamento()));

        // Valores
        m.valorBoleto                  = nvlBd(dto.getValorDocumento());
        m.valorCobrado                 = m.valorBoleto;
        m.valorDescontos               = BigDecimal.ZERO;
        m.valorDeducoes                = BigDecimal.ZERO;
        m.valorMulta                   = BigDecimal.ZERO;
        m.valorAcrescimos              = BigDecimal.ZERO;
        m.quantidadeMoeda              = BigDecimal.ZERO;
        m.valorMoeda                   = BigDecimal.ZERO;

        // Título
        m.especieDocumento             = dto.getEspecie() != null ? dto.getEspecie() : "DV";
        m.especieMoeda                 = "R$";
        m.aceite                       = "S".equalsIgnoreCase(dto.getAceite());
        m.localPagamento               = dto.getLocalPagamento() != null
                                            ? dto.getLocalPagamento()
                                            : "Pagável em qualquer banco até o vencimento.";
        m.numeroDocumento              = nvl(dto.getNumeroDocumento());
        m.instrucoes                   = dto.getInstrucoes() != null
                                            ? dto.getInstrucoes()
                                            : Collections.emptyList();

        // Barras (preenchidos pelo connector pós-registro)
        m.linhaDigitavel               = nvl(dto.getLinhaDigitavel());
        m.codigoDeBarras               = nvl(dto.getCodigoBarras());

        // Carnê
        m.parcela                      = dto.getParcela()       != null ? dto.getParcela().toString()       : "";
        m.totalParcelas                = dto.getTotalParcelas() != null ? dto.getTotalParcelas().toString()  : "";

        return m;
    }

    // -----------------------------------------------------------------------
    // Helpers privados
    // -----------------------------------------------------------------------

    private static String formatarAgenciaConta(BoletoDTO dto) {
        StringBuilder sb = new StringBuilder();
        if (dto.getAgencia() != null)              sb.append(dto.getAgencia());
        if (dto.getDigitoAgencia() != null)        sb.append("-").append(dto.getDigitoAgencia());
        sb.append(" / ");
        if (dto.getCodigoBeneficiario() != null)   sb.append(dto.getCodigoBeneficiario());
        if (dto.getDigitoCodigoBeneficiario() != null) sb.append("-").append(dto.getDigitoCodigoBeneficiario());
        return sb.toString();
    }

    private static URL resolverLogoBanco(String codigoBanco) {
        if (codigoBanco == null) return null;
        String path = "/br/com/zeusapp/stella/core/images/banco-" + codigoBanco + ".png";
        return BoletoReportModel.class.getResource(path);
    }

    private static Date toDate(LocalDate ld) {
        if (ld == null) return null;
        return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static LocalDate nvlDate(LocalDate ld) {
        return ld != null ? ld : LocalDate.now();
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }

    private static BigDecimal nvlBd(BigDecimal bd) {
        return bd != null ? bd : BigDecimal.ZERO;
    }

    // -----------------------------------------------------------------------
    // Getters — gerados manualmente para evitar dependência de Lombok no modelo
    // interno (Lombok está como optional; o template usa reflexão para chamá-los)
    // -----------------------------------------------------------------------

    public String getNomeBeneficiario()           { return nomeBeneficiario; }
    public String getDocumentoBeneficiario()      { return documentoBeneficiario; }
    public String getEnderecoBeneficiario()       { return enderecoBeneficiario; }
    public String getAgenciaECodigoBeneficiario() { return agenciaECodigoBeneficiario; }
    public String getNossoNumeroECodDocumento()   { return nossoNumeroECodDocumento; }
    public String getDigitoNossoNumero()          { return digitoNossoNumero; }
    public String getCarteira()                   { return carteira; }
    public URL    getLogoBanco()                  { return logoBanco; }
    public String getNumeroBancoComDigito()       { return numeroBancoComDigito; }

    public String getNomePagador()                { return nomePagador; }
    public String getDocumentoPagador()           { return documentoPagador; }
    public String getLogradouroPagador()          { return logradouroPagador; }
    public String getBairroPagador()              { return bairroPagador; }
    public String getCepPagador()                 { return cepPagador; }
    public String getCidadePagador()              { return cidadePagador; }
    public String getUfPagador()                  { return ufPagador; }

    public Date      getVencimento()              { return vencimento; }
    public Date      getDataDocumento()           { return dataDocumento; }
    public Date      getDataProcessamento()       { return dataProcessamento; }

    public BigDecimal getValorBoleto()            { return valorBoleto; }
    public BigDecimal getValorCobrado()           { return valorCobrado; }
    public BigDecimal getValorDescontos()         { return valorDescontos; }
    public BigDecimal getValorDeducoes()          { return valorDeducoes; }
    public BigDecimal getValorMulta()             { return valorMulta; }
    public BigDecimal getValorAcrescimos()        { return valorAcrescimos; }
    public BigDecimal getQuantidadeMoeda()        { return quantidadeMoeda; }
    public BigDecimal getValorMoeda()             { return valorMoeda; }

    public String       getEspecieDocumento()     { return especieDocumento; }
    public String       getEspecieMoeda()         { return especieMoeda; }
    public Boolean      getAceite()               { return aceite; }
    public String       getLocalPagamento()       { return localPagamento; }
    public String       getNumeroDocumento()      { return numeroDocumento; }
    public List<String> getInstrucoes()           { return instrucoes; }

    public String getLinhaDigitavel()             { return linhaDigitavel; }
    public String getCodigoDeBarras()             { return codigoDeBarras; }

    public String getParcela()                    { return parcela; }
    public String getTotalParcelas()              { return totalParcelas; }
}
