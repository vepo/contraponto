package dev.vepo.contraponto.readinglist;

public record ReadingListActionView(long postId,
                                    Long itemId,
                                    ReadingListItemState state,
                                    boolean authenticated,
                                    boolean showControls) {

    public boolean notSaved() {
        return state == ReadingListItemState.NOT_SAVED;
    }

    public boolean unread() {
        return state == ReadingListItemState.UNREAD;
    }

    public boolean read() {
        return state == ReadingListItemState.READ;
    }

    public static ReadingListActionView guest(long postId) {
        return new ReadingListActionView(postId, null, ReadingListItemState.NOT_SAVED, false, true);
    }

    public static ReadingListActionView forUser(long postId, Long itemId, ReadingListItemState state) {
        return new ReadingListActionView(postId, itemId, state, true, true);
    }

    public static ReadingListActionView hidden(long postId) {
        return new ReadingListActionView(postId, null, ReadingListItemState.NOT_SAVED, false, false);
    }
}
