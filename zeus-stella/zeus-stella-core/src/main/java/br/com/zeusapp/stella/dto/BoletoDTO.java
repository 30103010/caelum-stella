package br.com.zeusapp.stella.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoletoDTO {

    // -----------------------------------------------------------------------
    // Beneficiário (quem recebe o pagamento)
    // -----------------------------------------------------------------------
    private String nomeBeneficiario;
    private String cnpjCpfBeneficiario;
    private String enderecoBeneficiario;
    private String agencia;
    private String digitoAgencia;
    private String codigoBeneficiario;
    private String digitoCodigoBeneficiario;
    private String carteira;
    private String convenio;

    // -----------------------------------------------------------------------
    // Pagador (quem paga)
    // -----------------------------------------------------------------------
    private String nomePagador;
    private String cpfCnpjPagador;
    private String enderecoPagador;

    // -----------------------------------------------------------------------
    // Título
    // -----------------------------------------------------------------------
    private BigDecimal valorDocumento;
    private LocalDate vencimento;
    private LocalDate dataDocumento;
    private LocalDate dataProcessamento;
    private String numeroDocumento;
    private String nossoNumero;
    private String especie;
    private String aceite;
    private String usoBanco;
    private List<String> instrucoes;
    private String localPagamento;

    // -----------------------------------------------------------------------
    // Carnê — null para boleto simples
    // -----------------------------------------------------------------------
    private Integer parcela;
    private Integer totalParcelas;

    // -----------------------------------------------------------------------
    // PIX — se emvPix != null, o renderer inclui o QR Code no boleto
    // -----------------------------------------------------------------------
    private String emvPix;
    private String txid;

    // -----------------------------------------------------------------------
    // Campos preenchidos pelo BancoConnector após registro
    // (null = boleto ainda não registrado ou banco não fornece)
    // -----------------------------------------------------------------------
    private String linhaDigitavel;
    private String codigoBarras;

    // -----------------------------------------------------------------------
    // Banco: "001"=BB, "237"=Bradesco, "341"=Itaú, "748"=Sicoob
    // -----------------------------------------------------------------------
    private String codigoBanco;

    // -----------------------------------------------------------------------
    // Layout: "PADRAO", "CARNE", "PIX", "FATURA", "ENTREGA"
    // -----------------------------------------------------------------------
    private String tipoLayout;
}
