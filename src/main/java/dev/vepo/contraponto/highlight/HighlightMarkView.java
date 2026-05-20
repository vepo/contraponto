package dev.vepo.contraponto.highlight;

public record HighlightMarkView(long id,
                                String passage,
                                String anchorJson,
                                String anchorClusterHash,
                                boolean personal,
                                boolean official,
                                boolean ownHighlight,
                                String notePreview) {

    public boolean noted() {
        return notePreview != null && !notePreview.isBlank();
    }
}
