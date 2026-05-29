package br.com.zeusapp.stella.connector.exception;

public class RegistroBoletoException extends BancoConnectorException {

    public RegistroBoletoException(String codigoBanco, String message, int httpStatus) {
        super(codigoBanco, message, httpStatus);
    }

    public RegistroBoletoException(String codigoBanco, String message, int httpStatus, Throwable cause) {
        super(codigoBanco, message, httpStatus, cause);
    }
}
