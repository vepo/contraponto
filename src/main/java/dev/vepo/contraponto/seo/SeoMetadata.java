package dev.vepo.contraponto.seo;

import static java.util.Objects.requireNonNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import io.quarkus.qute.RawString;

public record SeoMetadata(String title,
                          String description,
                          String canonicalUrl,
                          SeoOgType ogType,
                          Optional<String> ogImageUrl,
                          boolean noindex,
                          Optional<String> jsonLd,
                          Optional<LocalDateTime> articlePublishedAt,
                          Optional<LocalDateTime> articleModifiedAt) {

    public SeoMetadata {
        title = title != null ? title : "Contraponto";
        description = description != null ? description : "";
        canonicalUrl = canonicalUrl != null ? canonicalUrl : "";
        ogType = ogType != null ? ogType : SeoOgType.WEBSITE;
        requireNonNull(ogImageUrl, "SeoMetadata.ogImageUrl must not be null");
        requireNonNull(jsonLd, "SeoMetadata.jsonLd must not be null");
        requireNonNull(articlePublishedAt, "SeoMetadata.articlePublishedAt must not be null");
        requireNonNull(articleModifiedAt, "SeoMetadata.articleModifiedAt must not be null");

    }

    public SeoMetadata(String title,
                       String description,
                       String canonicalUrl,
                       SeoOgType ogType,
                       Optional<String> ogImageUrl,
                       boolean noindex,
                       Optional<String> jsonLd,
                       Optional<LocalDateTime> articlePublishedAt) {
        this(title, description, canonicalUrl, ogType, ogImageUrl, noindex, jsonLd, articlePublishedAt, Optional.empty());
    }

    public Optional<RawString> jsonLdRaw() {
        return jsonLd.filter(json -> !json.isBlank()).map(RawString::new);
    }

    public Optional<String> articleModifiedAtIso() {
        return articleModifiedAt.map(DateTimeFormatter.ISO_LOCAL_DATE_TIME::format);
    }

    public Optional<String> articlePublishedAtIso() {
        return articlePublishedAt.map(DateTimeFormatter.ISO_LOCAL_DATE_TIME::format);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String title = "Contraponto";
        private String description = "";
        private String canonicalUrl = "";
        private SeoOgType ogType = SeoOgType.WEBSITE;
        private Optional<String> ogImageUrl = Optional.empty();
        private boolean noindex;
        private Optional<String> jsonLd = Optional.empty();
        private Optional<LocalDateTime> articlePublishedAt = Optional.empty();
        private Optional<LocalDateTime> articleModifiedAt = Optional.empty();

        public Builder articleModifiedAt(LocalDateTime articleModifiedAt) {
            this.articleModifiedAt = Optional.ofNullable(articleModifiedAt);
            return this;
        }

        public Builder articlePublishedAt(LocalDateTime articlePublishedAt) {
            this.articlePublishedAt = Optional.ofNullable(articlePublishedAt);
            return this;
        }

        public SeoMetadata build() {
            return new SeoMetadata(title,
                                   description,
                                   canonicalUrl,
                                   ogType,
                                   ogImageUrl,
                                   noindex,
                                   jsonLd,
                                   articlePublishedAt,
                                   articleModifiedAt);
        }

        public Builder canonicalUrl(String canonicalUrl) {
            this.canonicalUrl = canonicalUrl;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder jsonLd(String jsonLd) {
            this.jsonLd = Optional.ofNullable(jsonLd);
            return this;
        }

        public Builder noindex(boolean noindex) {
            this.noindex = noindex;
            return this;
        }

        public Builder ogImageUrl(String ogImageUrl) {
            this.ogImageUrl = Optional.ofNullable(ogImageUrl);
            return this;
        }

        public Builder ogType(SeoOgType ogType) {
            this.ogType = ogType;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }
    }
}
