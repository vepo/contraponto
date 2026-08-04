# Post reactions

**Feature version:** 1
**Status:** review-ready
**Production:** not yet

## Changelog

### End-of-post engagement widget — 2026-08-04

**Version:** 1
**Status:** review-ready

**Production:** not yet — new capability

**Description:** Add an engagement widget at the end of every published post with two parts: (1) a discussion prompt inviting the reader to comment on the site or continue the conversation on Reddit (via a Reddit search link for the post's canonical URL), and (2) a reaction bar letting signed-in readers react to the post with one or more of a fixed set of emoji (👍 like, 👎 dislike, 😄 smile, 🎉 party, 😢 sad, ❤️ love, 🚀 rocket, 😟 worried), GitHub-issue-reaction style — each reaction type toggles independently per reader. Anonymous readers see the counts but cannot react.

**Domain model:** updated 2026-08-04 — see `docs/domain-specification.md` § Post reactions.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| `post-comments` | Discussion prompt links to the existing lazy `#comments` fragment — no change to comment behaviour |
| `post-publishing` | Widget renders on `PostEndpoint` post view, after article content |
| ActivityPub / Fediverse favourites | Displayed **like** (👍) count = stored `LIKE` reactions **+** existing Fediverse favourite count (FQ6) — read-only aggregation, no schema link between the two |
| `notification` | New `NEW_REACTION` type, one row per **add** (not per removal), following the `NEW_COMMENT` pattern |

## Summary

Signed reader-engagement nudge shown once, at the end of the post body (after `<article class="article-page__content">`, before the highlights/comments lazy sections). Two independent pieces bundled in one visual widget:

1. **Discuss prompt** — short copy + a link that jumps to `#comments` on the same page, and a link to a Reddit search results page scoped to the post's canonical URL (`https://reddit.com/search/?q=url:<post absolute url>`), opened in a new tab. No schema; reuses the existing `ShareLinks`/`ShareView` machinery that already builds LinkedIn/Bluesky share URLs from the post's absolute URL.
2. **Reactions** — a row of 8 emoji buttons, always visible with live counts. Signed-in readers toggle any subset on/off independently (e.g. "like" **and** "rocket" on the same post); anonymous readers see the same counts read-only — clicking prompts sign-in (same `loginRequired` HTMX gate used elsewhere). The post's own author cannot react to their own post (button disabled/hidden for the author). The **like** count folds in the existing Fediverse ♥ favourite count.

## Wireframe

| Screen | Route | Notes |
|--------|-------|-------|
| Post page — engagement widget | `GET /{username}/post/{slug}` (existing `PostEndpoint`) | New static block after article content: discuss prompt (2 links) + reaction bar fragment container |
| Reaction bar fragment | `GET …/components/reactions` | Lazy-loaded like reading list / highlights; re-fetched on auth change |
| Reaction toggle | `POST /forms/posts/{postId}/reactions/{type}` | `@Logged`; toggles the type for the current reader, returns the refreshed reaction bar fragment |

```
┌───────────────────────────────────────────────────┐
│  [article content ends here]                        │
│                                                       │
│  Gostou do que leu? Comente aqui ou continue a       │
│  conversa no Reddit.                                 │
│  [ Comentar aqui ]   [ Discutir no Reddit ]          │
│                                                       │
│  👍 12   👎 1   😄 4   🎉 2   😢 0   ❤️ 7   🚀 3   😟 0│
│  (reader's own active reactions highlighted; author  │
│   viewing own post sees counts only, no buttons)     │
└───────────────────────────────────────────────────┘
```

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | New `reaction` package under **Reader engagement** context (alongside `comment`, `highlight`); discuss-prompt half touches `post`/`shared.share` only, no schema. Like-count aggregation reads from `activitypub.inbox.ActivityPubFavouriteService` (Integration context) directly from the `reaction` endpoints — same cross-context read pattern `PostEndpoint` already uses for the favourites view (see AQ1, resolved during implementation) |
| Schema | New table `tb_post_reactions`: `id`, `post_id` FK, `user_id` FK, `reaction_type` varchar, `created_at`; unique `(post_id, user_id, reaction_type)` |
| `dev-import.sql` | Seed a handful of reactions (mixed types) on an existing demo post |
| Notifications | New `NotificationType.NEW_REACTION`, fired on add only, to the post's author |
| Tests | `ShareLinksTest` (Reddit URL), `PostReactionServiceTest` (unit), `PostReactionWebTest` (web), `BoundedContextRulesTest` stays green |

### Risks

| Risk | Mitigation |
|------|------------|
| Like count double-counts if a reader is both a local reactor and (hypothetically) the same person's remote Fediverse actor | Accepted — favourites are anonymous remote actors, no identity link to local users exists or is planned |
| Reddit search link 404s / empty results for posts with few backlinks | Acceptable — it's a search, not a guaranteed thread; copy sets expectations, no functional mitigation needed |
| Notification volume: a reader toggling several types quickly generates several `NEW_REACTION` rows | Acceptable — mirrors `NEW_COMMENT` volume; no batching in v1 |

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Do reactions require sign-in (same gate as comments), or can anonymous readers react? | answered | **Sign-in required to react; anonymous readers see the total counts (read-only)** |
| FQ2 | Can a reader activate multiple reaction types on the same post (GitHub-style independent toggles), or is it mutually exclusive? | answered | **Yes — independent toggles, GitHub-style** |
| FQ3 | Can a reader remove/change a reaction after giving it? | answered | **Yes — toggle off removes it** |
| FQ4 | Are reaction counts visible to anonymous visitors, or only to signed-in readers? | answered | **Visible to everyone** |
| FQ5 | Can the post's own author react to their own post? | answered | **No** |
| FQ6 | How do reactions relate to the existing Fediverse ♥ favourites count shown in post metadata? | answered | **Fediverse favourites count as a "like" reaction** — folded into the 👍 total |
| FQ7 | Does the post author get a notification (like `NEW_COMMENT`) when a reader reacts? | answered | **Yes** |
| FQ8 | Exact discuss-prompt copy (pt-BR default + en/es `i18n` keys) | answered | **Use PO-drafted copy** (finalized below in HTMX/i18n notes) |

## Architecture

### ADRs aplicáveis

| ADR | Relevância |
|-----|------------|
| [0003](../docs/adr/0003-frontend-qute-htmx.md) | Qute + HTMX fragment for the reaction bar, same pattern as reading list / highlights |
| [0005](../docs/adr/0005-postgresql-database.md) | New Flyway migration `V0.0.16__post_reactions.sql` |

No new ADR required — this reuses accepted, transversal decisions; nothing here is a new cross-cutting architectural stance.

### Design específico da feature

**Bounded context:** new `dev.vepo.contraponto.reaction` package, **Reader engagement** context (alongside `comment`, `highlight`, `readinglist`).

**Schema** (`V0.0.16__post_reactions.sql`, mirrors `tb_post_comments` / `tb_activitypub_favourites` conventions):

```sql
CREATE TABLE tb_post_reactions (
    id              BIGSERIAL PRIMARY KEY,
    post_id         BIGINT NOT NULL REFERENCES tb_posts(id) ON DELETE CASCADE,
    user_id         BIGINT NOT NULL REFERENCES tb_users(id) ON DELETE CASCADE,
    reaction_type   VARCHAR(20) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (post_id, user_id, reaction_type)
);

CREATE INDEX idx_post_reactions_post ON tb_post_reactions (post_id);
```

**Domain types:**

- `ReactionType` enum — `LIKE, DISLIKE, SMILE, PARTY, SAD, LOVE, ROCKET, WORRIED` (glyph + i18n label via `@TemplateExtension` in `reaction` package).
- `PostReaction` entity — `id`, `post` (`@ManyToOne`), `user` (`@ManyToOne`), `reactionType` (`@Enumerated(STRING)`), `createdAt` — same shape/style as `PostComment`.
- `PostReactionRepository` — `countsByPost(postId): Map<ReactionType, Long>`, `findActiveTypes(postId, userId): Set<ReactionType>`, `exists(postId, userId, type)`, `save`, `delete`.
- `PostReactionService.toggle(post, user, type)` — throws `BadRequestException` if `user.equals(post.getAuthor())` (FQ5); inserts if absent (fires `NotificationService.notifyNewReaction`), deletes if present (no notification); returns the refreshed `PostReactionBarView`.
- `PostReactionBarView(List<ReactionCountView> reactions, boolean authenticated, boolean canReact)` where `ReactionCountView(ReactionType type, String emoji, long count, boolean active)`. `canReact = authenticated && !isPostAuthor`.
- **Like count union:** `PostReactionBarView.withAdditionalLikeCount(long)` adds the Fediverse favourite count to the `LIKE` entry only; called from `PostReactionComponentEndpoint`/`PostReactionFormEndpoint` (both inject `ActivityPubFavouriteService` directly, using `buildPostView(post)` so hidden/opted-out favourites are never added) — same cross-context read `PostEndpoint` already performs today for `fediverseFavourites` (AQ1, resolved: isolated to the endpoint/HTTP-boundary layer, not `PostReactionService`, so the service itself has no dependency on `activitypub`).

**Notification:** add `NEW_REACTION` to `NotificationType`; `linkUrl` case reuses `postLink(notification)` (anchor to the widget can be added client-side later if needed — not blocking). `NotificationService.notifyNewReaction(User recipient, Post post, User actor, ReactionType type)` called from `PostReactionService` on add, mirroring `PostCommentService` → `notifyNewComment`.

**Endpoints** (mirrors `ReadingListComponentEndpoint` / `ReadingListSaveEndpoint` split):

- `PostReactionComponentEndpoint` — `GET {username}/post/{slug}/components/reactions` and the secondary-blog variant — public, renders the fragment (`Templates.reactionBar(view)`).
- `PostReactionFormEndpoint` — `@Logged @POST /forms/posts/{postId}/reactions/{type}` — toggles, returns the refreshed fragment (no `Toast`, mirrors the self-contained swap style of `PostEndpoint/toggle.html`, not the `Toast`-based `ReadingListMutationResponse` style, since there's no cross-page state to reconcile).

**Discuss prompt (no schema):**

- `ShareView` gains `redditUrl`; `ShareLinks.from(title, absoluteUrl)` adds `REDDIT_SEARCH_BASE = "https://www.reddit.com/search/?q="` and builds `REDDIT_SEARCH_BASE + encode("url:" + trimmedUrl)`.
- New `components/discuss-widget.html` partial, included from `post.html` right after `</article>`, using the existing `share` view model already passed into `Templates.post(...)`.

**Copy (FQ8, pt-BR default + i18n keys, en/es added to `messages_en.json` / `messages_es.json`):**

| Key | pt-BR (hardcoded default) | en | es |
|-----|---------------------------|----|----|
| `discuss.prompt` | Gostou do que leu? Comente aqui ou continue a conversa no Reddit. | Liked what you read? Comment here or keep the conversation going on Reddit. | ¿Te gustó lo que leíste? Comenta aquí o sigue la conversación en Reddit. |
| `discuss.commentHere` | Comentar aqui | Comment here | Comentar aquí |
| `discuss.reddit` | Discutir no Reddit | Discuss on Reddit | Discutir en Reddit |
| `reaction.signInHint` | Entre para reagir | Sign in to react | Inicia sesión para reaccionar |
| `reaction.like` / `.dislike` / `.smile` / `.party` / `.sad` / `.love` / `.rocket` / `.worried` | Curtir / Não curtir / Sorrir / Comemorar / Triste / Amei / Foguete / Preocupado | Like / Dislike / Smile / Party / Sad / Love / Rocket / Worried | Me gusta / No me gusta / Sonreír / Fiesta / Triste / Me encanta / Cohete / Preocupado |

### HTMX component model

| Component | Fragment route | Activator | Target / swap | Events out / in | JS | Auth allowlist |
|-----------|-----------------|-----------|----------------|------------------|----|-----------------|
| Discuss prompt | none (static, rendered inline in `post.html`) | page load | n/a | none | none | public |
| Reaction bar | `GET {username}/post/{slug}/components/reactions` (+ secondary-blog variant) | `hx-trigger="load, {authRefreshTrigger}"` on `#post-reactions` | `#post-reactions` / `innerHTML` | in: `loggedIn from:body, loggedOut from:body` (existing `AUTH_REFRESH_TRIGGER`) | none | public read |
| Reaction toggle button | `POST /forms/posts/{postId}/reactions/{type}` | click on a reaction button | `#post-reactions` / `innerHTML` (self-contained response) | on 401: `HX-Trigger-After-Settle: loginRequired` (existing mechanism, opens login modal) | none | `@Logged` |

No new custom event added to `docs/htmx-events.md` — reuses the documented `AUTH_REFRESH_TRIGGER` (§1) and `loginRequired` (§2 auth-sensitive chrome) mechanisms already in place for reading list / highlights / comments.

### Architecture questions (AQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | Like-count aggregation reads `activitypub.inbox.ActivityPubFavouriteService` from the new `reaction` package (Reader engagement → Integration), a pair not listed in the domain-spec dependency table — `PostEndpoint` (Content publishing → Integration) already does the analogous cross-context read for the favourites view today. Accept the same precedent for `reaction`, or compute the union elsewhere? | resolved | The reaction bar is its own independently-fetched HTMX fragment (initial load **and** every toggle response), not embedded only at full-page render time, so composing the union in `PostEndpoint` alone doesn't cover it. Isolated the `activitypub` dependency to `PostReactionComponentEndpoint`/`PostReactionFormEndpoint` (HTTP boundary) — `PostReactionService` and the rest of the `reaction` package stay free of any `activitypub` import |
| AQ2 | `dev-import.sql` seed — which demo post(s) get seeded reactions? | resolved | Seeded on `introduction-to-distributed-systems-java` (alice's post): `dave` → LIKE, `bob` → LIKE, `eve` → ROCKET + LOVE, plus one `NEW_REACTION` notification for alice |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Discuss prompt renders after article content with working "Comment here" (`#comments`) and Reddit search links | Summary | ☑ |
| FC2 | Reaction bar renders 8 types with live counts, visible to anonymous readers | FQ1, FQ4 | ☑ |
| FC3 | Signed-in, non-author reader can toggle any subset of reaction types independently | FQ2, FQ5 | ☑ |
| FC4 | Reader can remove a previously given reaction | FQ3 | ☑ |
| FC5 | Anonymous reader clicking a reaction button is prompted to sign in (no reaction recorded) | FQ1 | ☑ (buttons are static spans for anonymous readers — no POST is even reachable client-side; `@Logged` still gates the endpoint server-side) |
| FC6 | Post author sees counts only, no reaction buttons, on their own post | FQ5 | ☑ |
| FC7 | Displayed **like** count = stored `LIKE` reactions + Fediverse favourite count | FQ6 | ☑ |
| FC8 | Author receives a `NEW_REACTION` notification when a reader adds a reaction (not on removal) | FQ7 | ☑ |
| FCdev | `dev-import.sql` seeds non-empty reaction counts on a demo post | dev-import-sql-safety | ☑ |

#### Tasks (phase 3) — layer tags

| ID | Layer | Task | Depends | Expected outcome | Tests | Done |
|----|-------|------|---------|-------------------|-------|------|
| T1-java | java | `ShareView`/`ShareLinks`: add `redditUrl` (`https://www.reddit.com/search/?q=` + encoded `url:<absolute post url>`) | — | `ShareLinks.from(...)` returns a working Reddit search URL alongside LinkedIn/Bluesky | TC1 | ☑ |
| T1-htmx | htmx | `components/discuss-widget.html` partial (comment-here + Reddit links, pt-BR copy + `data-i18n`), included in `post.html` after `</article>`; CSS; `discuss.*` i18n keys in `messages_en.json`/`messages_es.json` | T1-java | Discuss prompt renders at the end of every post, links work | TC2 | ☑ |
| T2-java | java | `V0.0.16__post_reactions.sql` (`tb_post_reactions`); `ReactionType` enum; `PostReaction` entity; `PostReactionRepository` (`countsByPost`, `findActiveTypes`, `find`, `save`, `delete`) | — | Schema + persistence primitives for post reactions | TC3 | ☑ |
| T3-java | java | `PostReactionService.toggle(post, user, type)` (self-react guard FQ5, idempotent add/remove); `NotificationType.NEW_REACTION` + `NotificationService.notifyNewReaction`; `PostReactionBarView`/`ReactionCountView` | T2-java | Toggle persists/removes a reaction, notifies author on add only, self-react rejected | TC3 | ☑ |
| T3-htmx | htmx | `PostReactionComponentEndpoint` (`GET …/components/reactions` + secondary-blog variant); `reactionBar.html` fragment; wire `#post-reactions` into `post.html` (`hx-trigger="load, {authRefreshTrigger}"`); endpoint composes like-count union (AQ1) | T3-java, T1-htmx | Reaction bar renders with live counts (incl. Fediverse favourites folded into like) for anon and signed-in readers | TC4 | ☑ |
| T4-java | java | `PostReactionFormEndpoint` (`@Logged POST /forms/posts/{postId}/reactions/{type}`, toggles, returns refreshed fragment) | T3-java | Signed-in non-author reader can react/unreact; anonymous gets `loginRequired` gate | TC4 | ☑ |
| T4-htmx | htmx | Reaction buttons (`hx-post` toggle, active-state styling), hidden/disabled for the post author, `reaction.*` i18n keys, `reaction.signInHint` copy | T4-java, T3-htmx | Full toggle UX matches FC2–FC6 | TC4 | ☑ |
| Tdev | java | `dev-import.sql` seed rows on `introduction-to-distributed-systems-java` (AQ2); `docs/feature-catalog.md` Social + Dev personas rows | T2-java | Dev persona sees a populated reaction bar + discuss prompt out of the box | — | ☑ |

#### Test coverage (phase 3)

| ID | Kind | Test | Covers | Done |
|----|------|------|--------|------|
| TC1 | unit | `ShareLinksTest` — Reddit URL assertions | T1-java | ☑ |
| TC2 | web | `PostShareWebTest#publishedPostDiscussWidgetLinksToCommentsAndReddit` — discuss prompt links present, `href`s correct | T1-htmx | ☑ |
| TC3 | rest | `PostReactionTest` (`@QuarkusIntegrationTest`) — self-react rejected (400), notification fired once on add and not on removal | T2-java, T3-java | ☑ |
| TC4 | rest | `PostReactionTest` — anonymous sees static counts + sign-in hint, no reachable toggle; signed-in reader holds multiple independent types; post page wires `#post-reactions` fragment | T3-htmx, T4-java, T4-htmx | ☑ |

**Development approval:** approved 2026-08-04 — "Approve t1-Tdev" (T1-java, T1-htmx, T2-java, T3-java, T3-htmx, T4-java, T4-htmx, Tdev)

**Review approval:** not yet — squad implementation complete, `GITHUB_ACTIONS=true ./mvnw -B verify` green (980 tests, 0 failures), `spotless:check` clean; awaiting phase 6 review + phase 7 human sign-off.

**Implementation notes:**

- New package `dev.vepo.contraponto.reaction`: `ReactionType`, `PostReaction`, `PostReactionRepository`, `PostReactionService`, `ReactionCountView`, `PostReactionBarView`, `PostReactionComponentEndpoint` (+ `reactionBar.html`), `PostReactionFormEndpoint`.
- `V0.0.16__post_reactions.sql` adds `tb_post_reactions`.
- `NotificationType.NEW_REACTION` added (`NotificationType`, `NotificationService.notifyNewReaction`, `NotificationTemplateExtensions`).
- `ShareView`/`ShareLinks` gained `redditUrl`; new `components/discuss-widget.html` included in `post.html` inside a new `<section class="post-engagement">` wrapping both the discuss prompt and the `#post-reactions` fragment container, right after the article body.
- AQ1 resolved differently from the original phase-2 recommendation once the HTMX fragment shape was implemented — see AQ1 row above.
- Verified `dev-import.sql` end-to-end via a real `quarkus:dev` boot (seed executed without SQL errors; unrelated pre-existing Git-remote-auth warnings in the log are not from this change).
- Test coverage: `ShareLinksTest`, `PostShareWebTest#publishedPostDiscussWidgetLinksToCommentsAndReddit`, new `PostReactionTest` (`@QuarkusIntegrationTest`, 5 tests). `BoundedContextRulesTest` and the rest of the suite stay green.
- `docs/domain-specification.md` § Post reactions added (terms, notification type, invariants 46–49); `.cursor/rules/contraponto-bounded-contexts.mdc` and the bounded-context table note `reaction` as planned→now implemented; `docs/feature-catalog.md` Social section + Dev personas updated; `.cursor/rules/dev-import-sql-safety.mdc` coverage registry row added.
