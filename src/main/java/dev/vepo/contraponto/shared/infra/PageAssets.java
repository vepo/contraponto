package dev.vepo.contraponto.shared.infra;

/**
 * Which optional styles and scripts belong in the page {@code <head>} for the
 * current request.
 */
public enum PageAssets {

    PUBLIC_READ(false, false, false),
    POST_READ(false, true, false),
    WRITE(true, true, false),
    MANAGE(false, false, true);

    private final boolean write;
    private final boolean post;
    private final boolean manage;

    PageAssets(boolean write, boolean post, boolean manage) {
        this.write = write;
        this.post = post;
        this.manage = manage;
    }

    public boolean isManage() {
        return manage;
    }

    public boolean isPost() {
        return post;
    }

    public boolean isWrite() {
        return write;
    }
}
