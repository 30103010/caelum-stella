package br.com.zeusapp.stella.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoletoRegistradoDTO {

    private String linhaDigitavel;
    private String codigoBarras;
    private String nossoNumeroFormatado;

    /** String EMV do QR Code Pix (null quando o boleto não tem Pix) */
    private String emvPix;

    /** ID da transação Pix no banco */
    private String txid;

    /** URL da imagem do QR Code — alguns bancos (BB, Sicoob) fornecem diretamente */
    private String qrCodeUrl;

    private LocalDateTime dataRegistro;
}
