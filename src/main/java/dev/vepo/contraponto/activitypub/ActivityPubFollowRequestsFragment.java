package dev.vepo.contraponto.activitypub;

final class ActivityPubFollowRequestsFragment {

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    static String render(ActivityPubFederationView view) {
        if (view.pendingFollowRequests().isEmpty()) {
            return """
                   <p class="form-group__hint">No pending Fediverse follow requests.</p>
                   """;
        }
        var rows = new StringBuilder();
        for (var request : view.pendingFollowRequests()) {
            rows.append("""
                        <div class="activitypub-follow-request">
                          <span class="activitypub-follow-request__handle">%s</span>
                          <button type="button" class="btn btn--primary btn--sm"
                                  hx-post="/forms/writing/activitypub/follows/%d/accept"
                                  hx-target="#activitypubFollowRequests"
                                  hx-swap="innerHTML">Accept</button>
                          <button type="button" class="btn btn--secondary btn--sm"
                                  hx-post="/forms/writing/activitypub/follows/%d/reject"
                                  hx-target="#activitypubFollowRequests"
                                  hx-swap="innerHTML">Reject</button>
                        </div>
                        """.formatted(escapeHtml(request.remoteActorId()), request.followId(), request.followId()));
        }
        return rows.toString();
    }

    private ActivityPubFollowRequestsFragment() {}
}
