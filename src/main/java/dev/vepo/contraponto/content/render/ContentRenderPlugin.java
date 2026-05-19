package dev.vepo.contraponto.content.render;

import java.util.List;

/**
 * SPI for turning {@code {% identifier param … %} render tags} in post body
 * into HTML. Implementations are discovered via
 * {@link java.util.ServiceLoader}.
 */
public interface ContentRenderPlugin {

    String identifier();

    /**
     * @param params whitespace-separated parameters from the render tag
     * @return safe HTML fragment, or empty to leave the tag unchanged
     */
    String render(List<String> params);
}
