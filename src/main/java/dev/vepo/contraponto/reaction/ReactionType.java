package dev.vepo.contraponto.reaction;

import java.util.Locale;

public enum ReactionType {

    LIKE("👍", "reaction.like", "Curtir"),
    DISLIKE("👎", "reaction.dislike", "Não curtir"),
    SMILE("😄", "reaction.smile", "Sorrir"),
    PARTY("🎉", "reaction.party", "Comemorar"),
    SAD("😢", "reaction.sad", "Triste"),
    LOVE("❤️", "reaction.love", "Amei"),
    ROCKET("🚀", "reaction.rocket", "Foguete"),
    WORRIED("😟", "reaction.worried", "Preocupado");

    private final String emoji;
    private final String i18nKey;
    private final String defaultLabel;

    ReactionType(String emoji, String i18nKey, String defaultLabel) {
        this.emoji = emoji;
        this.i18nKey = i18nKey;
        this.defaultLabel = defaultLabel;
    }

    public String defaultLabel() {
        return defaultLabel;
    }

    public String emoji() {
        return emoji;
    }

    public String i18nKey() {
        return i18nKey;
    }

    public String slug() {
        return name().toLowerCase(Locale.ROOT);
    }
}
