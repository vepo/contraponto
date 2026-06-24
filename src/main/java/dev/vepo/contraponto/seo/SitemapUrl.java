package dev.vepo.contraponto.seo;

import java.time.LocalDateTime;
import java.util.Optional;

public record SitemapUrl(String loc, Optional<LocalDateTime> lastModified, Optional<String> imagePath) {

    public SitemapUrl(String loc, Optional<LocalDateTime> lastModified) {
        this(loc, lastModified, Optional.empty());
    }
}
