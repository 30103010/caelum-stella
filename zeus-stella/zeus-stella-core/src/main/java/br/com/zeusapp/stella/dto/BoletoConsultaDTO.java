package br.com.zeusapp.stella.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoletoConsultaDTO {

    private String nossoNumero;
    private String linhaDigitavel;
    private String codigoBarras;
    private BigDecimal valor;
    private LocalDate vencimento;

    /**
     * Situação normalizada pela lib: "A_RECEBER", "LIQUIDADO", "BAIXADO", "VENCIDO".
     * Cada connector mapeia os códigos proprietários do banco para estes valores.
     */
    private String situacao;

    private LocalDate dataPagamento;
    private BigDecimal valorPago;
    private String emvPix;
    private String txid;
    private LocalDateTime dataConsulta;
}
