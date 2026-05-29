package br.com.zeusapp.stella.connector.exception;

public class ConsultaBoletoException extends BancoConnectorException {

    public ConsultaBoletoException(String codigoBanco, String message, int httpStatus) {
        super(codigoBanco, message, httpStatus);
    }

    public ConsultaBoletoException(String codigoBanco, String message, int httpStatus, Throwable cause) {
        super(codigoBanco, message, httpStatus, cause);
    }
}
