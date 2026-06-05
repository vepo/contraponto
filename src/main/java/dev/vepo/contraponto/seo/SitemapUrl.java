package dev.vepo.contraponto.seo;

import java.time.LocalDateTime;
import java.util.Optional;

public record SitemapUrl(String path, Optional<LocalDateTime> lastModified, Optional<String> imagePath) {

    public SitemapUrl(String path, Optional<LocalDateTime> lastModified) {
        this(path, lastModified, Optional.empty());
    }
}
