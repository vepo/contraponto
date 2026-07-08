package dev.vepo.contraponto.activitypub.inbox;

import dev.vepo.contraponto.activitypub.actor.ActivityPubFederationView;

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
                        <div class="fediverse__follow-request">
                          <div class="fediverse__follow-request-meta">
                            <span class="fediverse__follow-request-name">%s</span>
                            <span class="fediverse__follow-request-handle">%s</span>
                            <a class="fediverse__follow-request-actor-link" href="%s" rel="noopener noreferrer" target="_blank">%s</a>
                          </div>
                          <div class="fediverse__follow-request-actions">
                            <button type="button" class="btn btn--primary btn--sm"
                                    hx-post="/forms/writing/activitypub/follows/%d/accept"
                                    hx-target="#activitypubFollowRequests"
                                    hx-swap="innerHTML">Accept</button>
                            <button type="button" class="btn btn--secondary btn--sm"
                                    hx-post="/forms/writing/activitypub/follows/%d/reject"
                                    hx-target="#activitypubFollowRequests"
                                    hx-swap="innerHTML">Reject</button>
                          </div>
                        </div>
                        """.formatted(escapeHtml(request.displayName()),
                                      escapeHtml(request.remoteHandle()),
                                      escapeHtml(request.remoteActorId()),
                                      escapeHtml(request.remoteActorId()),
                                      request.followId(),
                                      request.followId()));
        }
        return rows.toString();
    }

    private ActivityPubFollowRequestsFragment() {}
}
