package dev.vepo.contraponto.activitypub;

import java.util.List;

public record FediverseFavouritePostView(boolean visible, long count, List<String> remoteHandles) {

    public static FediverseFavouritePostView hidden() {
        return new FediverseFavouritePostView(false, 0, List.of());
    }
}
