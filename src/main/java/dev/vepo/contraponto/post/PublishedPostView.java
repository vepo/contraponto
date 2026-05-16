package dev.vepo.contraponto.post;

import java.util.Objects;

public record PublishedPostView(Post post, PostPublication live) {

    public PublishedPostView {
        Objects.requireNonNull(post, "post");
    }

    public boolean hasLivePublication() {
        return live != null;
    }
}
