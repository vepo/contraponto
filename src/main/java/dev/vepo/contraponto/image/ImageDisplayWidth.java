package dev.vepo.contraponto.image;

/**
 * Target display widths for resized image variants ({@code ?w=} on
 * {@code /api/images/}).
 */
public enum ImageDisplayWidth {

    /** Article and blog cards (~280px layout, 2× for retina). */
    CARD(560),

    /** Blog directory banner (~280px layout, 2× for retina). */
    BANNER(560);

    private final int pixels;

    ImageDisplayWidth(int pixels) {
        this.pixels = pixels;
    }

    public int pixels() {
        return pixels;
    }
}
