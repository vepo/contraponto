# Reddit account linking and outbound syndication

> **Status**: Accepted
>
> **Updated**: 2026-08-04
>
> **Aceitação / reabertura:** somente **manual** pelo usuário humano. O agente cria com `Proposed` e **não** muda para `Accepted` ou `Reopened` sem mensagem explícita (ex.: "Aceito o ADR-0018", "Reabro o ADR-0018 porque …"). Ver [development-process.mdc](../../.cursor/rules/development-process.mdc).

## Summary

Authors link their **personal Reddit account** to Contraponto via standard OAuth2 (authorization-code flow, `duration=permanent`) from **Author appearance**. Contraponto stores the resulting Reddit username and an **encrypted refresh token**, scoped `identity submit`. Per **blog** they own, authors may enable **Reddit syndication**; each **publish** submits a Reddit **link post** to the author's own profile (`sr=u_{redditUsername}`). This is the app's **first per-user OAuth integration** and **first per-user secret stored at rest** — every prior integration (Bluesky, ActivityPub) uses platform-wide env credentials only.

## Drivers

* Product decision ([feature/reddit-syndication.md](../../feature/reddit-syndication.md) FQ1): syndication must post as the **author's own** Reddit account, not a shared platform bot — a single bot account submitting many authors' links reads as spam to Reddit and risks a ban.
* Reddit's API has no equivalent to Bluesky's app-password or ActivityPub's server keypair — a **per-user OAuth token** is unavoidable for this product shape.
* [ADR-0017](0017-per-blog-git-credentials-ssh.md) already establishes the precedent that each integration's stored secret uses **its own** encryption key, never shared — this decision must not create a shortcut around that.
* `authentication.md`'s baseline states "No OAuth/MFA — out of scope MVP"; this ADR scopes OAuth narrowly to **Reddit account linking**, not platform login, to avoid silently expanding that baseline.

## Options

### A — Reuse ActivityPub's key-encryption secret for the Reddit refresh token

No new config; encrypt the refresh token with `contraponto.activitypub.key-encryption-secret`.

### B — Dedicated per-user Reddit OAuth integration, own encryption secret, async delivery queue

New `reddit` bounded context. `UserRedditAccount` stores Reddit username + AES-GCM-encrypted refresh token under a **new**, Reddit-only secret (`contraponto.reddit.token-encryption-secret`). Standard OAuth2 authorization-code flow against a platform-registered Reddit "web app" (client id/secret in `%prod` env, not per-user). Outbound submission/delete via an async DB queue + scheduler, mirroring `ActivityPubDeliveryService`/`ActivityPubDeliveryObserver`.

### C — Short-lived access token only, no persisted refresh token

Do not request `duration=permanent`; require the author to be interactively present (browser session with a fresh Reddit token) for every publish.

## Options Analysis

### A Assessment

* Pro: Zero new config.
* Con: Rotating ActivityPub's secret (e.g. after a suspected leak in that unrelated integration) would silently break every author's Reddit link; couples two independent trust boundaries. Contradicts the separate-secret precedent set by ADR-0017.

### B Assessment

* Pro: Matches the product decision (per-author account, FQ1); isolates blast radius per integration; reuses the async-queue + CDI-event pattern already proven for ActivityPub deliveries, so no new architectural mechanism.
* Con: Introduces the app's first per-user OAuth token lifecycle (authorize, refresh, revoke) and a third encrypted-secret-at-rest subsystem to operate; requires registering a Reddit API app before any deployment can use the feature.

### C Assessment

* Pro: No persisted secret at all.
* Con: **Rejected outright** — it cannot deliver the core product requirement of posting automatically at publish time when the author is not necessarily online; defeats the feature.

## Recommendation

Adopt **option B**.

| Concern | Decision |
|---------|----------|
| Scope | Reddit account link on **User** — one per user, table `tb_user_reddit_accounts` |
| OAuth flow | Standard authorization-code grant against a Reddit "web app" (confidential client); platform-wide client id + secret in `%prod` env, **not** per-user |
| Token duration | Request `duration=permanent` to obtain a refresh token; short-lived (~1h) access token refreshed on demand before each delivery, not persisted |
| What is encrypted | Refresh token only — AES-GCM, same transform shape as `ActivityPubKeyPairService` but a **separate** class (`RedditTokenCipherService`) and a **separate** secret |
| Crypto secret | `contraponto.reddit.token-encryption-secret` — new, independent of ActivityPub's and Git's (ADR-0017) secrets |
| Scopes requested | Minimal: `identity` (resolve Reddit username) + `submit` (create/delete link posts) |
| Delivery | Async DB queue + scheduled worker, mirroring `ActivityPubDeliveryService`/`GitSyncRun`; `RedditDeliveryObserver` observes `PostPublishedEvent` / `PostUnpublishedEvent` with `@Transactional(REQUIRES_NEW)` on `AFTER_SUCCESS`, same shape as `ActivityPubDeliveryObserver` |
| Submission | `POST /api/submit` — link post, `sr=u_{redditUsername}`, title truncated to Reddit's title limit, `url` = canonical post URL |
| Deletion | `POST /api/del` using the stored submission fullname (`RedditPostRecord`) on unpublish |
| Rate limits | Respect Reddit's OAuth API rate-limit response headers; backoff + retry in the delivery worker, same failure/retry shape as existing delivery queues |
| Revocation | Disconnect calls Reddit's `POST /api/v1/revoke_token`, then deletes the stored ciphertext row; cascades `Blog.redditSyndicationEnabled = false` on every blog owned by that user and raises an in-app notice |
| User-Agent | Descriptive custom `User-Agent` per Reddit API rules (`platform:app-id:version (by /u/operator)`) — required or Reddit rate-limits/blocks generic user agents |

### Consequences

* Pro: Delivers the exact per-author profile-post feature scoped in `feature/reddit-syndication.md` without inventing a new architectural mechanism — reuses the CDI-event + async-queue pattern already proven for ActivityPub.
* Pro: Isolates blast radius — a Reddit secret compromise or rotation cannot affect ActivityPub or (future) Git credentials, and vice versa.
* Con: Operators must register a Reddit API app and set a new encryption secret before any author can link an account (deployment checklist addition).
* Con: Adds a third encrypted-secret-at-rest subsystem (ActivityPub keys, Git credentials — proposed, Reddit tokens) with duplicated AES-GCM boilerplate across `ActivityPubKeyPairService`-shaped classes; consolidating into one shared cipher utility is a reasonable future refactor but explicitly **out of scope** here — do not block this ADR on it.
* Other: Complements [ADR-0002](0002-backend-java-quarkus-jakarta-ee.md) (Quarkus REST client for the Reddit API), [ADR-0005](0005-postgresql-database.md) (Flyway columns + queue table), [ADR-0013](0013-cdi-events-cross-context.md) (publish/unpublish observer pattern), and follows the separate-secret-per-integration precedent from [ADR-0017](0017-per-blog-git-credentials-ssh.md).

### Confirmation

* Unit: refresh-token encrypt/decrypt round-trip; submission-title truncation; disconnect cascades `redditSyndicationEnabled = false` on all owned blogs.
* Integration: mocked Reddit OAuth token endpoint + `/api/submit` + `/api/del`; end-to-end publish → enqueue → deliver → `RedditPostRecord` stored; unpublish → delete.
* Regression: blog toggle stays disabled when the owner has no linked account; a failed Reddit delivery does not roll back or block the publish transaction (verified the same way as `ActivityPubDeliveryObserver`'s `REQUIRES_NEW` isolation).

## Changelog

| Data | Evento | Detalhe |
|------|--------|---------|
| 2026-08-04 | proposed | Per-author Reddit OAuth account linking; encrypted refresh token with a dedicated secret; async link-post delivery to the author's own Reddit profile. |
| 2026-08-04 | accepted | Aceite manual do usuário. |

## More Information

* [feature/reddit-syndication.md](../../feature/reddit-syndication.md)
* Encryption pattern reference: `ActivityPubKeyPairService` (separate secret, not reused directly)
* Sibling precedent: [ADR-0017](0017-per-blog-git-credentials-ssh.md) — per-integration encrypted secrets, no cross-integration fallback
* Delivery pattern reference: `ActivityPubDeliveryService` / `ActivityPubDeliveryObserver`
