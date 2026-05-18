package dev.vepo.contraponto.image;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RenderedHtmlEnricher {

    private final ImageAltEnricher imageAltEnricher;

    @Inject
    public RenderedHtmlEnricher(ImageAltEnricher imageAltEnricher) {
        this.imageAltEnricher = imageAltEnricher;
    }

    public String enrichHtml(String html) {
        return imageAltEnricher.enrichHtml(html);
    }
}
