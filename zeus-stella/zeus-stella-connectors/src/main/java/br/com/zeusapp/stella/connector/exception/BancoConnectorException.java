package br.com.zeusapp.stella.connector.exception;

public class BancoConnectorException extends RuntimeException {

    private final String codigoBanco;
    private final int httpStatus;

    public BancoConnectorException(String codigoBanco, String message, int httpStatus) {
        super(message);
        this.codigoBanco = codigoBanco;
        this.httpStatus = httpStatus;
    }

    public BancoConnectorException(String codigoBanco, String message, int httpStatus, Throwable cause) {
        super(message, cause);
        this.codigoBanco = codigoBanco;
        this.httpStatus = httpStatus;
    }

    public String getCodigoBanco() {
        return codigoBanco;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
