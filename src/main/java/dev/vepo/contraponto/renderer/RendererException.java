package dev.vepo.contraponto.renderer;

public class RendererException extends RuntimeException {
    public RendererException(String message) {
        super(message);
    }

    public RendererException(String message, Exception cause) {
        super(message, cause);
    }
}
