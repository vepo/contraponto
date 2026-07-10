package dev.vepo.contraponto.activitypub.inbox;

public record FediverseFavouritePostView(boolean visible, long count) {

    public static FediverseFavouritePostView hidden() {
        return new FediverseFavouritePostView(false, 0);
    }
}
