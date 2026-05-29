package br.com.zeusapp.stella.connector.exception;

public class AutenticacaoException extends BancoConnectorException {

    public AutenticacaoException(String codigoBanco, String message) {
        super(codigoBanco, message, 401);
    }

    public AutenticacaoException(String codigoBanco, String message, Throwable cause) {
        super(codigoBanco, message, 401, cause);
    }
}
