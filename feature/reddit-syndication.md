# Reddit post syndication

**Feature version:** 1  
**Status:** review-ready  
**Production:** not deployed

## Changelog

### Reddit post syndication — 2026-08-04

**Version:** 1  
**Status:** review-ready

**Description:** Authors **link their personal Reddit account** to Contraponto via OAuth from **account appearance settings**. Per **blog** they own, they may enable a toggle (disabled by default, and disabled/greyed until a Reddit account is linked) that submits each **publish** as a Reddit **link post** to **their own Reddit profile** (`u_<username>`), pointing at the canonical post URL.

**Domain model:** updated 2026-08-04 — added **Reddit account link**, **Reddit syndication enabled**, **Reddit syndication delivery**, **Reddit post record**, **Reddit account disconnect** to [docs/domain-specification.md](../docs/domain-specification.md) § Reddit syndication; added `reddit` *(planned)* to the Integration bounded context table.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Publish / republish | Create Reddit link post when blog toggle on **and** owner has linked account; republish never re-touches Reddit (FQ6) |
| Unpublish | Delete Reddit submission when post unpublished (FQ7) |
| Blog manage / edit | New Reddit section (checkbox, disabled until linked) beside Git sync |
| Author appearance | New "Connect Reddit account" section (OAuth link/unlink); disconnecting auto-disables the toggle on every owned blog + notifies (FQ8) |
| Notifications | New type `REDDIT_SYNDICATION_FAILED` after failed delivery (FQ9); notice on disconnect-triggered auto-disable |
| ActivityPub Fediverse / Bluesky | **Independent** — separate per-blog toggle, different account model (platform vs per-author) |
| Share actions | Unchanged — manual "Share to Reddit" link, if any, remains |
| RSS / SEO | Same canonical post URL used as the Reddit submission link |
| Platform admin | Kill-switch to globally disable Reddit syndication (FQ10) |
| Authentication baseline | `authentication.md` currently states "No OAuth/MFA — out of scope MVP"; this is the **first OAuth integration** in the app, scoped to Reddit account linking only (not login) |
| Deployment | New Reddit API app registration (client id/secret, redirect URI, user-agent string) + `%prod` env vars; new encrypted token storage |

## Summary

Enable authors to auto-share new posts to their **own Reddit profile** by linking their personal Reddit account once (OAuth, from account appearance settings), then opting in **per blog** they own. When enabled, each **publish** on that blog submits a Reddit **link post** (title + canonical post URL) to the author's own profile — a Reddit "profile post" (`sr=u_<their-username>`) — visible on their Reddit profile page and to anyone following them there, **not** to any subreddit's front page or subreddit audience.

**Not in scope for v1:**

- Posting to subreddits (author-chosen or platform-owned)
- Updating/re-syndicating the Reddit post when the source post is retitled or edited (only unpublish removes it — FQ6/FQ7)
- Flair, NSFW/spoiler tagging, crossposting, self-text posts
- Any platform-owned Reddit bot account
- Multiple Reddit accounts per author

**Depends on (Accepted ADRs):**

- [ADR-0002](../docs/adr/0002-backend-java-quarkus-jakarta-ee.md) — Quarkus OIDC/OAuth2 client + REST client for Reddit API
- [ADR-0003](../docs/adr/0003-frontend-qute-htmx.md) — Appearance settings + blog settings UI
- [ADR-0005](../docs/adr/0005-postgresql-database.md) — Linked-account + delivery record storage
- [ADR-0013](../docs/adr/0013-cdi-events-cross-context.md) — `PostPublishedEvent` / `PostUnpublishedEvent` observers

**New ADR (phase 2):** needed. This is the app's **first per-user OAuth integration and first per-user secret stored at rest** — existing integrations (Bluesky, ActivityPub) use platform-wide env credentials only. Architect should decide whether to extend `ActivityPubKeyPairService`'s AES-GCM pattern, or the sibling pattern proposed in [ADR-0017](../docs/adr/0017-per-blog-git-credentials-ssh.md) (also Proposed, not yet implemented), for encrypting the stored Reddit refresh token.

## Wireframe

| Field | Value |
|-------|-------|
| **Source** | ASCII below |
| **Last updated** | 2026-08-04 |

### Screen: Author appearance settings — Connect Reddit account

Placed in `AuthorAppearanceEndpoint/panel.html` (writing hub → Appearance panel). Saved/actioned via a new form endpoint alongside `AuthorAppearanceUpdateEndpoint` (`/forms/writing/appearance`).

```
┌─ Reddit account ─────────────────────────────────────────────┐
│ Connect your Reddit account to share new posts to your        │
│ own Reddit profile when you publish.                          │
│                                                                 │
│ Not connected.                                                 │
│ [ Connect Reddit account ]  → Reddit OAuth consent screen      │
│                                                                 │
│ (after connecting)                                             │
│ Connected as u/exampleuser                                     │
│ [ Disconnect ]                                                 │
│   Disconnecting turns off Reddit syndication on every blog     │
│   you own.                                                     │
└─────────────────────────────────────────────────────────────┘
```

### Screen: Blog edit — Reddit syndication section

Placed beside Git sync in `BlogManageEndpoint/form.html`, new sibling partial `redditSyndicationSection.html`.

```
┌─ Reddit ────────────────────────────────────────────────────┐
│ Share new posts on this blog to your own Reddit profile      │
│ when you publish.                                            │
│                                                                │
│ [ ] Enable Reddit syndication for this blog                  │
│     (off by default)                                         │
│                                                                │
│ (disabled state — no linked account)                         │
│ Connect a Reddit account in Appearance settings to enable     │
│ this. [ Go to Appearance → ]                                  │
└───────────────────────────────────────────────────────────┘
```

### Screen: N/A — post editor

No change. Publish triggers syndication automatically when the blog toggle is on and the owner has a linked account.

### Screen: N/A — public reader surfaces

No Reddit badge on post pages in v1.

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | New **`reddit`** under **Integration**; observes `post` / `blog` / `user`; notifies via `notification` on failure |
| Packages | `dev.vepo.contraponto.reddit` |
| API / routes | OAuth connect/callback under `/forms/writing/reddit/...`; disconnect action; blog save form extended with `reddit_enabled` |
| UI | Appearance panel Reddit section; blog edit Reddit fieldset |
| Schema | New `tb_user_reddit_accounts` (Reddit username, encrypted refresh token, linked_at); `tb_blogs.reddit_syndication_enabled`; `tb_reddit_post_records` (post id → Reddit submission id/permalink); delivery queue table |
| Notifications | `NotificationType.REDDIT_SYNDICATION_FAILED`; disconnect-triggered auto-disable notice |
| Config | New Reddit API app (client id/secret, redirect URI, user-agent string per Reddit API rules); `%prod` env vars; new token-encryption secret |
| **`dev-import.sql`** | Not applicable — no dev-mode OAuth support in v1 (AQ4/AQ6, user decision); schema column defaults (`reddit_syndication_enabled = false`) need no seed changes |
| Tests | Unit (submission builder, token refresh); integration (mock Reddit API); `@WebTest` for connect/disconnect UI and blog toggle |
| Docs | domain-spec, feature-catalog, deployment.md, docker-smoke README, ARCHITECTURE § syndication |

### Risks

| Risk | Mitigation |
|------|------------|
| **First per-user OAuth token stored at rest** | New ADR; AES-GCM encryption mirroring `ActivityPubKeyPairService` / ADR-0017; never log tokens |
| **Token revoked/expired at Reddit's end** | Delivery failure handling + notification (FQ9); author must reconnect |
| **Reddit API terms** — requires registered app, user-agent string, OAuth rate limits (~60 req/min) | Config + client built to Reddit API rules; queue + backoff |
| **Profile posts have low discovery/visibility** (no subreddit front-page reach) | Product trade-off already chosen; document expectation so authors aren't surprised by low reach |
| **Author disconnects while blog toggle on** | Auto-disable + notify (FQ8) prevents a silent broken state |
| **Stale Reddit post after retitle** (no update support v1) | Documented limitation (FQ6) |
| **Reddit account suspension/ban risk for the author** (automated posting from their own account) | Outside Contraponto's control; document that Reddit's self-promotion/bot rules apply to the author's own account |

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | **Account model:** platform-owned bot account vs per-author linked OAuth account? | answered | **Per-author linked account** |
| FQ2 | **Target:** fixed subreddit, author-chosen subreddit, or author's own profile? | answered | **Author's own Reddit profile** (`u_<username>` profile post) |
| FQ3 | **Post format:** link post vs self text post? | answered | **Link post** (title + canonical URL) |
| FQ4 | **Opt-in scope:** per blog vs per author account? | answered | **Per blog** (checkbox in blog edit, mirrors Git sync pattern) |
| FQ5 | Blog toggle behavior when owner has **no linked Reddit account**? | answered | **Disabled/greyed** with "connect first" hint, not just hidden |
| FQ6 | **Republish** with changed title after already syndicated: skip, or delete+recreate? | answered | **Skip** — leave the original Reddit post as-is (Reddit's API doesn't allow editing a link post's title/URL anyway) |
| FQ7 | **Unpublish:** delete the Reddit submission or leave it? | answered | **Delete** |
| FQ8 | **Disconnecting** a linked Reddit account: auto-disable toggle on all owned blogs + notify, or leave toggle on to fail silently? | answered | **Auto-disable + notify** |
| FQ9 | Notify author in-app when a Reddit delivery fails after retries? | open | Recommend **yes** — mirrors Bluesky FQ10 / Git sync failure notification pattern |
| FQ10 | Platform admin **kill-switch** to globally disable Reddit syndication? | open | Recommend **yes** — mirrors ActivityPub/Bluesky platform panel pattern |

**Gate:** phase 3 requires blocking **FQ*n*** answered or marked `not valid`. **FQ1–FQ8 answered**; FQ9–FQ10 open but non-blocking — architect may proceed with the stated recommendation, confirm with user in phase 2.

## Architecture

### ADRs aplicáveis

| ADR | Status | Relevância |
|-----|--------|------------|
| [0002](../docs/adr/0002-backend-java-quarkus-jakarta-ee.md) | Accepted | Quarkus REST client, OAuth2 authorization-code flow |
| [0003](../docs/adr/0003-frontend-qute-htmx.md) | Accepted | Appearance panel + blog edit forms |
| [0005](../docs/adr/0005-postgresql-database.md) | Accepted | Flyway + delivery queue table |
| [0013](../docs/adr/0013-cdi-events-cross-context.md) | Accepted | `PostPublishedEvent` / `PostUnpublishedEvent` observer |
| [0017](../docs/adr/0017-per-blog-git-credentials-ssh.md) | Proposed | Sibling precedent — separate encryption secret per integration, no fallback |
| [0018](../docs/adr/0018-reddit-account-linking-syndication.md) | **Accepted** (2026-08-04) | Reddit OAuth account linking, token encryption, outbound submission scope |

### Design específico da feature

| Area | Design |
|------|--------|
| **Bounded context** | `reddit` — Integration; depends on `shared`, `user`, `blog`, `post` |
| **Layers** | `RedditOAuthEndpoint` (connect + callback, full navigation) → `RedditAccountLinkService` → `RedditApiClient` (token exchange/refresh, submit, delete, revoke) → `UserRedditAccountRepository`; `RedditAccountDisconnectEndpoint`; `RedditDeliveryObserver` (mirrors `ActivityPubDeliveryObserver`) → `RedditDeliveryService` → `RedditDeliveryQueueRepository` → `RedditDeliveryScheduler`; `BlogManageEndpoint`/blog save form extended with `reddit_enabled`; `RedditPlatformSettingsEndpoint` (admin kill-switch) |
| **Events** | `PostPublishedEvent` → enqueue Reddit link-post submission when blog toggle on **and** owner has a linked account; `PostUnpublishedEvent` → enqueue delete of the stored `RedditPostRecord` |
| **OAuth** | Authorization-code grant, `duration=permanent`, scopes `identity submit`; `state` bound to session for CSRF; callback exchanges code → access + refresh token, resolves `/api/v1/me` username, encrypts + stores refresh token |
| **Token refresh** | Short-lived access token (~1h) refreshed on demand before each delivery; never persisted |
| **Crypto** | `RedditTokenCipherService` (AES-GCM, same transform shape as `ActivityPubKeyPairService`, separate class + separate secret per [ADR-0018](../docs/adr/0018-reddit-account-linking-syndication.md)); config `contraponto.reddit.token-encryption-secret` |
| **Submission** | `RedditSubmissionBuilder` — link post via `POST /api/submit`: `sr=u_{redditUsername}`, `title` (post title, truncated to Reddit's submission title limit), `url` (canonical post URL) |
| **Deletion** | `POST /api/del` with the stored submission fullname (`RedditPostRecord.fullname`) on unpublish |
| **Disconnect** | `POST /api/v1/revoke_token` + delete `UserRedditAccount` row; cascades `Blog.redditSyndicationEnabled = false` on every blog owned by that user; raises `NotificationType.REDDIT_ACCOUNT_DISCONNECTED` |
| **Failure notify** | `NotificationService.notifyRedditSyndicationFailed(blog, post)` after delivery queue exhausts retries (FQ9) |
| **Blog flag** | `Blog.redditSyndicationEnabled` — saved via the existing blog save form (`POST /forms/blogs`); server validates the owner has a linked account before persisting `true` |
| **Admin kill-switch** | `RedditPlatformSettingsEndpoint` / `RedditSettings.enabled()` mirrors `ActivityPubSettings` (FQ10) |
| **Config** | `%prod`: `contraponto.reddit.enabled`, `.client-id`, `.client-secret`, `.redirect-uri`, `.user-agent`, `.token-encryption-secret` |
| **Tests** | Unit (cipher round-trip, submission title truncation, disconnect cascade); integration (mocked Reddit OAuth + `/api/submit` + `/api/del`); `@WebTest` for connect/disconnect UI and blog toggle disabled-until-linked state |

### HTMX component model

| Component id | Fragment route | Activator | Target/swap | Events | JS | Auth allowlist |
|--------------|----------------|-----------|-------------|--------|-----|----------------|
| `#redditMessage` section (Appearance panel) | included in existing `GET /components/panel?panel=appearance` hub fragment | — | — | — | none | `@Logged` author (own account) |
| Connect button | `GET /forms/writing/reddit/connect` | plain `<a href>` — **full browser navigation, not `hx-get`** (must leave the HTMX request cycle to reach Reddit's consent screen) | full page (302 to Reddit) | — | none | `@Logged` |
| OAuth callback | `GET /forms/writing/reddit/callback?code=...&state=...` | Reddit's redirect back | full-page redirect to Appearance panel + `Toast` | — | none | `@Logged`, `state` must match session |
| Disconnect button | `POST /forms/writing/reddit/disconnect` | `hx-post` | `#redditMessage` (`innerHTML`) | — | none | `@Logged` owner |
| Reddit fieldset (blog edit) | inline in `BlogManageEndpoint/form.html`, new sibling partial `redditSyndicationSection.html` | submitted together with the existing blog save form | `main`, same swap as rest of the blog form | — | none | blog owner |
| Admin kill-switch panel | `GET`/`POST` `/forms/administration/reddit` | admin `hx-post` | panel region, mirrors ActivityPub platform panel | — | none | `ADMIN` |

Follows the same self-contained-section pattern already used for **Fediverse (ActivityPub)** in `AuthorAppearanceEndpoint/panel.html` (own `<form>`, own `hx-post`, own `#…Message` target) — no shared state with the main appearance-save form. No new custom `HtmxTriggers` events in v1: Reddit connect/disconnect state is local to the Appearance panel, and the blog toggle rides the existing blog-save `main` swap.

### HTMX interaction diagram

```
Author clicks "Connect Reddit account" (full navigation)
  → GET /forms/writing/reddit/connect → 302 to Reddit OAuth consent screen
  → Reddit redirects to GET /forms/writing/reddit/callback?code&state
  → RedditOAuthEndpoint exchanges code, resolves username, encrypts + stores refresh token
  → redirect to Appearance panel (Toast: "Connected as u/username")

Author saves blog edit form with "Enable Reddit syndication" checked
  → POST /forms/blogs/{id} → Blog.redditSyndicationEnabled = true
    (only persisted true if the owner has a linked Reddit account)

Author publishes post on that blog
  → PostPublicationService → PostPublishedEvent
  → RedditDeliveryObserver → enqueue delivery (RedditDeliveryService)
  → RedditDeliveryScheduler → refresh access token → POST /api/submit (link post)
  → RedditPostRecord stored (post id → submission fullname/permalink)

Author unpublishes
  → PostUnpublishedEvent → RedditDeliveryObserver → enqueue delete → POST /api/del

Delivery exhausted retries
  → NotificationService → author in-app notification (REDDIT_SYNDICATION_FAILED)

Author clicks "Disconnect" in Appearance panel
  → POST /forms/writing/reddit/disconnect
  → POST /api/v1/revoke_token + delete UserRedditAccount
  → Blog.redditSyndicationEnabled = false on every blog owned by the author
  → in-app notice (REDDIT_ACCOUNT_DISCONNECTED) + Toast confirmation
```

### Architecture questions (AQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | Package name: `reddit` (matches single-word `git`/`activitypub` convention)? | answered | **Yes** — `dev.vepo.contraponto.reddit` |
| AQ2 | Delivery mechanism: async DB queue + scheduler (parity with ActivityPub/Bluesky) vs synchronous on `PostPublishedEvent`? | answered | **Async** — mirrors `ActivityPubDeliveryService` exactly, no new mechanism |
| AQ3 | Platform Reddit API app (client id/secret) location: `%prod` env + new `RedditSettings` config class mirroring `ActivityPubSettings`? | answered | **Yes** |
| AQ4 | Dev mode: real Reddit OAuth needs a registered app + public callback; how do developers exercise Connect/Disconnect locally? | **not valid** | **No dev-mode support needed** — Connect/Disconnect and delivery are only exercised in environments with a real registered Reddit app (`%prod`-style config); no localhost callback setup, no mock OAuth provider, no dev-setup doc for this |
| AQ5 | New ADR needed for the per-user OAuth token + encryption scope? | answered | **Yes** — [ADR-0018](../docs/adr/0018-reddit-account-linking-syndication.md), drafted `Proposed` |
| AQ6 | `dev-import.sql` happy-path seed: a real linked account needs live OAuth, which seed SQL can't perform. How to satisfy the mandatory dev-seed rule? | **not valid** | **No dev-seed needed** — no placeholder linked-account row; `dev-import.sql` only needs the plain schema defaults (`reddit_syndication_enabled` defaults `false`), nothing feature-specific. Blog toggle and Appearance "Connect" UI are visible in dev mode but stay inert (no linked account) until a real Reddit app is configured |

**Gate:** AQ1–AQ3, AQ5 answered; AQ4 and AQ6 marked **not valid** by explicit user decision (no dev-mode OAuth support scoped for v1). Ready for phase 3.

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Author can connect a Reddit account from Appearance settings (OAuth) | FQ1 | ☑ |
| FC2 | Blog edit shows the Reddit toggle, disabled/greyed until the owner has linked an account | FQ4, FQ5 | ☑ |
| FC3 | Publish on an enabled blog (owner linked) creates a Reddit link post on the author's own profile | FQ2, FQ3 | ☑ (outbound Reddit HTTP call itself not exercised in tests — see TC8 note, matches ActivityPub precedent) |
| FC4 | Toggle off → no delivery on publish | FQ4 | ☑ |
| FC5 | Republish with a changed title does not re-touch the existing Reddit post | FQ6 | ☑ (bug found and fixed during review pass — see Implementation notes) |
| FC6 | Unpublish deletes the Reddit submission | FQ7 | ☑ |
| FC7 | Disconnecting the Reddit account auto-disables the toggle on every blog the author owns and notifies them | FQ8 | ☑ |
| FC8 | Author is notified in-app after a Reddit delivery fails and retries are exhausted | FQ9 | ☑ |
| FC9 | Platform admin kill-switch halts all Reddit syndication | FQ10 | ☑ |
| FCdev | Not applicable — no dev-mode OAuth support, no placeholder seed row (AQ4, AQ6, user decision) | dev-import-sql-safety | N/A |

#### Tasks (phase 3)

| ID | Layer | Task | Depends | Expected outcome | Tests | Done |
|----|-------|------|---------|-------------------|-------|------|
| T1-java | java | `V____reddit_syndication.sql` (Flyway): `tb_user_reddit_accounts` (user_id unique FK, reddit_username, encrypted_refresh_token, linked_at), `tb_blogs.reddit_syndication_enabled` (default `false`), `tb_reddit_post_records` (post_id unique FK, submission_fullname, permalink, created_at), `tb_reddit_deliveries` (delivery queue: post_id, blog_id, kind CREATE\|DELETE, status, attempts, next_attempt_at, last_error); entities + repositories | — | Schema + persistence primitives for account link, delivery queue, and post records | TC1 (indirect, via later tasks) | ☑ |
| T2-java | java | `RedditSettings` (mirrors `ActivityPubSettings`: client-id, client-secret, redirect-uri, user-agent, token-encryption-secret, `enabled()`); `RedditTokenCipherService` (AES-GCM refresh-token encrypt/decrypt, mirrors `ActivityPubKeyPairService`) | — | Config + crypto primitives ready; refresh token never persisted in plaintext | TC1 | ☑ |
| T3-java | java | `RedditApiClient` (Quarkus REST client): exchange authorization code, refresh access token, `GET /api/v1/me`, `POST /api/submit`, `POST /api/del`, `POST /api/v1/revoke_token`; custom `User-Agent` per Reddit API rules | — | Thin, mockable wrapper around every Reddit HTTP call the feature needs | TC3 (mocked) | ☑ |
| T4-java | java | `RedditOAuthEndpoint`: `GET /forms/writing/reddit/connect` (session-bound `state`, 302 to Reddit authorize URL, `duration=permanent`, scopes `identity submit`); `GET /forms/writing/reddit/callback` (validates `state`, exchanges code via T3, resolves username, encrypts token via T2, upserts `UserRedditAccount`, redirects to Appearance panel + `Toast`) | T1-java, T2-java, T3-java | Author can complete the Reddit OAuth round trip and end up linked | TC3 | ☑ |
| T5-java | java | `RedditAccountDisconnectEndpoint`: `POST /forms/writing/reddit/disconnect` — revoke token (T3), delete `UserRedditAccount`, cascade `Blog.redditSyndicationEnabled = false` on every blog owned by the user, `NotificationService.notifyRedditAccountDisconnected` | T4-java | Disconnecting leaves no dangling enabled toggle and notifies the author | TC4 | ☑ |
| T5-htmx | htmx | Appearance panel Reddit section in `AuthorAppearanceEndpoint/panel.html` — self-contained `<form>`/block mirroring the existing Fediverse section (own `#redditMessage` target); "Not connected" + Connect link (`<a href>`, full navigation, **not** `hx-get`); "Connected as u/username" + Disconnect (`hx-post`); i18n keys | T4-java, T5-java | Author sees live connect/connected/disconnect state on the Appearance panel | TC5 | ☑ |
| T6-java | java | Extend blog save handling (`BlogManageEndpoint` / blog save form processing) with `reddit_enabled` bound to `Blog.redditSyndicationEnabled`; server rejects persisting `true` when the owner has no `UserRedditAccount` | T1-java, T4-java | Toggle can only end up `true` for an author who has actually linked an account, even if the client is bypassed | TC6 | ☑ |
| T6-htmx | htmx | `redditSyndicationSection.html` partial in `BlogManageEndpoint/form.html`, sibling to `gitSyncSection.html`; checkbox disabled/greyed with "connect first" hint + link to Appearance when the owner has no linked account | T6-java | Blog edit shows the Reddit toggle in the correct disabled/enabled state per FQ5 | TC7 | ☑ |
| T7-java | java | `RedditSubmissionBuilder` — builds the link-post payload (`sr=u_{username}`, title truncated to Reddit's submission title limit, canonical post URL) | T1-java | Pure, unit-testable submission payload construction | TC2 | ☑ |
| T8-java | java | `RedditDeliveryService` (`enqueueCreateForPublishedPost`, `enqueueDeleteForUnpublishedPost`) + `RedditDeliveryRepository` + `RedditDeliveryScheduler` (poll pending deliveries, refresh access token via T3, submit/delete, store `RedditPostRecord` on success, retry with backoff, mark failed after max attempts) | T1-java, T3-java, T7-java | Publish/unpublish deliveries actually reach Reddit, with retry and a durable post-record for later delete | TC8 | ☑ |
| T9-java | java | `RedditDeliveryObserver` (mirrors `ActivityPubDeliveryObserver`): `@Transactional(REQUIRES_NEW)` on `PostPublishedEvent`/`PostUnpublishedEvent` at `AFTER_SUCCESS`; guards on `RedditSettings.enabled()`, blog active, `Blog.redditSyndicationEnabled`, owner has a linked account; enqueues via T8 | T6-java, T8-java | Publish/unpublish on an opted-in blog reliably triggers (or skips) a Reddit delivery, isolated from the publishing transaction | TC8 | ☑ |
| T10-java | java | `NotificationType.REDDIT_SYNDICATION_FAILED` + `REDDIT_ACCOUNT_DISCONNECTED`; `NotificationService.notifyRedditSyndicationFailed` (after T8's scheduler exhausts retries) + link builders | T8-java | Author is notified in-app on delivery failure and on disconnect (FQ8, FQ9) | TC9 | ☑ |
| T11-java | java | `RedditPlatformSettingsEndpoint` (`GET`/`POST /forms/administration/reddit`), admin kill-switch flag consulted by `RedditSettings.enabled()` / `RedditDeliveryObserver`, mirroring `ActivityPubPlatformManageEndpoint` | T2-java | Admin can halt all outbound Reddit syndication instance-wide (FQ10) | TC10 | ☑ |
| T11-htmx | htmx | Admin panel Reddit kill-switch fieldset, mirrors `ActivityPubPlatformManageEndpoint/panel.html` | T11-java | Admin sees and can toggle the Reddit kill-switch on the platform insights/administration hub | TC11 | ☑ |
| T12-java | java | `BoundedContextRulesTest` extension: `reddit` context allowed deps (`shared`, `user`, `blog`, `post`, `notification`); `PackageSizeRulesTest` awareness if the package grows | T1-java…T11-java | Static architecture rules stay green for the new `reddit` package | TC12 | ☑ |
| Tdev | — | **Not applicable** — no `dev-import.sql` change (AQ4/AQ6, user decision: no dev-mode OAuth support). `docs/feature-catalog.md` entry added at phase 7 (docs-sync), not a squad task. | — | — | — | N/A |

**Squad order:** `T1-java → T2-java → T3-java → T4-java → T5-java → T5-htmx → T6-java → T6-htmx → T7-java → T8-java → T9-java → T10-java → T11-java → T11-htmx → T12-java`.

#### Test coverage (phase 3)

| ID | Kind | Test | Covers | Done |
|----|------|------|--------|------|
| TC1 | rest | `RedditTokenCipherServiceTest` (`@QuarkusIntegrationTest`) — encrypt/decrypt round trip, ciphertext doesn't leak plaintext, non-deterministic across calls | T2-java | ☑ |
| TC2 | unit | `RedditSubmissionBuilderTest` (`@UnitTest`, plain in-memory entities, no DB) — `sr=u_{username}`, URL composition, title truncation at 300 chars | T7-java | ☑ |
| TC3 | rest | `RedditOAuthEndpointTest` — connect redirect + callback `state` validation | T4-java | ☑ |
| TC4 | rest | `RedditAccountDisconnectEndpointTest` — revoke + delete, cascades toggle off, notifies | T5-java | ☑ — `AuthorAppearanceRedditWebTest#authorSeesConnectedStateAndCanDisconnect` drives the `POST /forms/writing/reddit/disconnect` HTTP endpoint itself through the browser (not just the service layer); `RedditDeliveryObserverTest#unlinkCascadesDisablesBlogToggle` covers the cascade at the service layer too |
| TC5 | web | `AuthorAppearanceRedditWebTest` — connect/connected/disconnect UI states | T5-htmx | ☑ |
| TC6 | rest | `BlogSaveRedditToggleTest` — toggle persists `true` only when linked; forged `true` from an unlinked owner rejected server-side | T6-java | ☑ |
| TC7 | web | `BlogManageRedditSectionWebTest` — fieldset disabled/hint when unlinked vs enabled when linked | T6-htmx | ☑ |
| TC8 | rest | `RedditDeliveryObserverTest` — publish/unpublish enqueue gating (toggle off, no linked account, toggle+linked → CREATE/DELETE enqueued with correct owner); republish-after-syndicated skip (FQ6) | T8-java, T9-java | ◐ enqueue path only — mirrors the same scope `ActivityPubDeliveryServiceTest` covers for ActivityPub; no test exercises `RedditDeliveryService.deliverOne` (actual Reddit HTTP submit/delete/retry), consistent with this repo's existing precedent of not exercising the outbound HTTP leg in automated tests |
| TC9 | rest | `RedditDeliveryFailureNotificationTest` — notification fires only after retries exhausted | T10-java | ☑ |
| TC10 | rest | `RedditPlatformSettingsEndpointTest` — admin can toggle the kill-switch; non-admin forbidden | T11-java | ☑ |
| TC11 | web | `RedditAdminPanelWebTest` — admin can view/toggle the kill-switch through `/administration/reddit`; non-admin sees no panel | T11-htmx | ☑ |
| TC12 | arch | `BoundedContextRulesTest` + `PackageSizeRulesTest` — generic rules pass for the new `reddit` package tree (`reddit`, `reddit.account`, `reddit.delivery`, `reddit.admin`, split to stay under the 25-type-per-package cap) | T12-java | ☑ |

**Development approval:** approved 2026-08-04 — tasks: T1-java, T2-java, T3-java, T4-java, T5-java, T5-htmx, T6-java, T6-htmx, T7-java, T8-java, T9-java, T10-java, T11-java, T11-htmx, T12-java (user: "Approve all")

**Implementation notes:** All 15 approved tasks implemented; all 12 test-coverage rows now have a written test (TC8 stays enqueue-path-only by established repo precedent — see row note). `GITHUB_ACTIONS=true mvn -o verify` (full CI gate: code style, unit/Quarkus/web suites, package/docker build) is green.

**Review pass (2026-08-04):** found and fixed one functional bug during review — `RedditDeliveryObserver.afterPublish` fired on every publish, including republish, with no guard in `RedditDeliveryService.enqueueCreateForPublishedPost` against a post that already has a `RedditPostRecord`. This meant republishing a post (e.g. a title edit) on an already-syndicated blog would create a **second, duplicate Reddit submission**, contradicting the already-answered FQ6 ("republish never re-touches Reddit") and FC5. Fixed by skipping enqueue when a post record already exists; added `RedditDeliveryObserverTest#republishDoesNotEnqueueCreateWhenPostAlreadySyndicated` as a regression test. Also fixed three import-hygiene violations (fully-qualified `MessageDigest`/`URLEncoder` used inline instead of imported) in `RedditOAuthStateService`, `RedditTokenCipherService`, `RedditApiClient`, per this repo's format-imports rule.

**Open, non-blocking note:** the Appearance-panel "Connect Reddit account" section (`RedditAppearanceService.buildView`) gates its visibility on `RedditSettings.enabled()`, which folds in the platform admin kill-switch. If an admin flips the kill-switch off, an author who already linked their account loses the "Connected as u/username" / "Disconnect" UI entirely (their link and delivery gating are unaffected — this is UI visibility only). The admin panel itself correctly separates this concern (`RedditPlatformManageEndpoint` gates on `configEnabled()`, not `enabled()`). Not covered by any FQ/AQ and not exercised by a test; flagging as a product-behavior question rather than changing it unilaterally.

**Status: `review-ready`** — ready for user review approval.

## Related capabilities

| Capability | Relationship |
|------------|--------------|
| [bluesky-platform-syndication.md](bluesky-platform-syndication.md) | Sibling syndication feature; different account model (platform-owned vs per-author) |
| [activitypub-integration.md](activitypub-integration.md) | Sibling; independent channel |
| [authentication.md](authentication.md) | Baseline states "No OAuth/MFA — out of scope MVP"; this feature is the first exception, scoped to Reddit account linking only, not login |
| [author-profile.md](author-profile.md) | New "Connect Reddit account" section in appearance settings |
| [git-sync.md](git-sync.md) | Same blog-settings toggle pattern |
