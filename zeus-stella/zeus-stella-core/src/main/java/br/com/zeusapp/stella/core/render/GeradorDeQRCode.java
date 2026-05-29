package br.com.zeusapp.stella.core.render;

import br.com.zeusapp.stella.core.exception.BoletoRenderException;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.ITFWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

/**
 * Utilitário para geração de imagens de código de barras e QR Code via ZXing.
 *
 * QR Code: padrão EMV Pix brasileiro (string emvPix do banco).
 * Código de barras: ITF (Interleaved 2 of 5), padrão FEBRABAN 44 dígitos.
 */
public class GeradorDeQRCode {

    /**
     * Gera um QR Code em PNG a partir da string EMV Pix.
     *
     * @param emv     string EMV completa retornada pelo banco
     * @param largura largura em pixels (recomendado: 200)
     * @param altura  altura em pixels (recomendado: 200)
     * @return PNG como byte[]
     */
    public byte[] gerarQRCode(String emv, int largura, int altura) {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        try {
            BitMatrix matrix = new QRCodeWriter().encode(emv, BarcodeFormat.QR_CODE, largura, altura, hints);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // MatrixToImageConfig() usa os padrões: preto (#FF000000) sobre branco (#FFFFFFFF)
            MatrixToImageWriter.writeToStream(matrix, "PNG", baos, new MatrixToImageConfig());
            return baos.toByteArray();
        } catch (WriterException | IOException e) {
            throw new BoletoRenderException("Erro ao gerar QR Code PIX", e);
        }
    }

    /**
     * Gera o código de barras FEBRABAN em PNG usando formato ITF (Interleaved 2 of 5).
     *
     * O código deve ter exatamente 44 dígitos numéricos (sem formatação).
     * Use largura alta para qualidade em PDF (ex: 800px).
     *
     * @param codigoDeBarras 44 dígitos numéricos sem espaços
     * @param largura        largura em pixels (recomendado: 800)
     * @param altura         altura em pixels (recomendado: 70)
     * @return PNG como byte[]
     */
    public byte[] gerarCodigoBarrasITF(String codigoDeBarras, int largura, int altura) {
        String digitos = codigoDeBarras.replaceAll("[^0-9]", "");
        if (digitos.isEmpty()) {
            throw new BoletoRenderException("Código de barras inválido: nenhum dígito encontrado");
        }

        // ITF requer número par de dígitos
        if (digitos.length() % 2 != 0) {
            digitos = "0" + digitos;
        }

        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.MARGIN, 0);

        // MatrixToImageConfig.BLACK = 0xFF000000 (int), WHITE = 0xFFFFFFFF (int)
        MatrixToImageConfig config = new MatrixToImageConfig(MatrixToImageConfig.BLACK, MatrixToImageConfig.WHITE);

        try {
            BitMatrix matrix = new ITFWriter().encode(digitos, BarcodeFormat.ITF, largura, altura, hints);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", baos, config);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new BoletoRenderException("Erro ao gerar código de barras ITF", e);
        } catch (Exception e) {
            // WriterException (se lançada por ITFWriter) ou outros erros de encoding
            throw new BoletoRenderException("Erro ao gerar código de barras ITF", e);
        }
    }
}
