package dev.vepo.contraponto.shared.htmx;

/**
 * HTMX response trigger headers and shared {@code hx-trigger} values.
 * <p>
 * {@code LOGGED_IN_ON_BODY} / {@code LOGGED_OUT_ON_BODY} dispatch on
 * {@code body} so subscribers with {@code loggedIn from:body} can refetch their
 * own fragment — not a full-page reload. See {@code docs/htmx-events.md}.
 */
public final class HtmxTriggers {

    public static final String HEADER_AFTER_SETTLE = "HX-Trigger-After-Settle";

    /** Menu OOB target on login / signup / logout responses. */
    public static final String MENU_CONTAINER_ID = "menu-container";

    /**
     * {@code hx-trigger} for elements that refetch on auth change (write button,
     * badge, in-page widgets). Not used on {@link #MENU_CONTAINER_ID} — menu is
     * updated via OOB in auth form responses.
     */
    public static final String AUTH_REFRESH_TRIGGER = "loggedIn from:body, loggedOut from:body";

    public static final String LOGGED_IN_ON_BODY = "{\"loggedIn\":{\"target\":\"body\"}}";

    public static final String LOGGED_OUT_ON_BODY = "{\"loggedOut\":{\"target\":\"body\"}}";

    private HtmxTriggers() {}
}
