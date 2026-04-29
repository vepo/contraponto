package dev.vepo.contraponto.renderer;

public sealed interface Renderer permits Markdown, AsciiDoctor {

    public static Renderer get(Format format) {
        return switch (format) {
            case MARKDOWN -> Markdown.getInstance();
            case ASCIIDOC -> AsciiDoctor.getInstance();
        };
    }

    String render(String content);
}
