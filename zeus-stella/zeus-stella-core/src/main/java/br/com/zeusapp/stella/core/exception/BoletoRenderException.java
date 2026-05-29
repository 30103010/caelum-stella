package br.com.zeusapp.stella.core.exception;

public class BoletoRenderException extends RuntimeException {

    public BoletoRenderException(String message) {
        super(message);
    }

    public BoletoRenderException(String message, Throwable cause) {
        super(message, cause);
    }
}
