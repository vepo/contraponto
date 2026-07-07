# ActivityPub actor identity and discovery

> **Status**: Accepted
>
> **Updated**: 2026-07-06

## Summary

Each **federation-enabled user** is represented by exactly one ActivityStreams **`Person`** actor — **1:1 with `User`**, not per blog. The actor's public identity is keyed to the user's **main blog** host and username. **Actor ID**, **WebFinger** `acct:` handle, and **preferredUsername** derive from `User.username` and the platform's **public HTTPS origin** (blog subdomain canonical host when enabled). **Secondary blogs** are not separate actors.

**Future (out of scope for MVP):** the same **User** actor may later emit activities for **post text highlights**, **comments**, and other engagement types — one identity, multiple activity kinds. That extension is **not** addressed in the first federation release.

## Drivers

* Mastodon users search **`@username@domain`** via WebFinger; actor `id` must be stable and HTTPS.
* Contraponto already distinguishes **platform host** vs **blog subdomain** ([domain-spec — Dual blog URL](../domain-specification.md)).
* One **user** may own multiple blogs; Fediverse users expect **one person, one follow** for that human — not one actor per blog.
* Reader **highlights** and **comments** are natural future activity types on the **same User actor**; defer to a later ADR/feature (not MVP).
* `User.mastodonUrl` today is only a **link-out**; federation requires **`sameAs`** optional link when the user also has a remote Mastodon account.

## Options

### One Person actor per user (main blog outbox in MVP)

Actor ID: `https://{username}.{base-domain}/` — one actor per **`User`**; outbox lists Creates for **main blog** posts in MVP; same actor may later carry highlights/comments activities.

### One actor per blog

Separate actors for secondary blogs (`architecture-notes`, etc.).

### Platform-wide single actor

One `@contraponto@site` actor for all authors — unsuitable for multi-author platform.

### Actor on platform host only (no subdomain)

All ActivityPub URLs on `APP_PUBLIC_URL`; post object IDs on subdomain.

## Options Analysis

### One Person per user Assessment

* Pro: Matches reader mental model (“follow Alice” the person, not “follow Alice's secondary blog”).
* Pro: Simpler WebFinger (`acct:alice@base-domain`); stable identity for future highlight/comment federation on the same actor.
* Con: Secondary blog posts either omitted from Fediverse MVP or included in the same user outbox with clear links.

### One actor per blog Assessment

* Pro: Precise blog-level follow.
* Con: Multiple WebFinger accounts per human; confusing on Mastodon; more UI/settings.

### Platform-wide actor Assessment

* Con: Not a publishing platform model.

### Platform host only Assessment

* Pro: Single TLS cert path for `.well-known`.
* Con: Diverges from **canonical subdomain URLs** used in SEO/RSS; object ID mismatch.

## Recommendation

**One `Person` actor per `User`** when **Fediverse publishing** is enabled on the account (at most one actor row per `user_id`).

| Field | Source |
|-------|--------|
| `id` | `https://{username}.{blog-subdomain-base}/` (same host as canonical post URLs) |
| `preferredUsername` | `User.username` |
| `name` | `User.name` |
| `summary` | `User.profileDescription` (plain text / stripped HTML) |
| `icon` | Avatar URL (`/components/avatar` or profile image) |
| `inbox` | `{actorId}inbox` |
| `outbox` | `{actorId}outbox` |
| `followers` / `following` | `{actorId}followers`, `{actorId}following` |
| `publicKey` | From [ADR-0007](0007-activitypub-http-signatures.md) |
| `url` | Author profile `GET /authors/{username}` on platform host |
| `sameAs` | Existing **author social links** including optional `mastodonUrl` |

**WebFinger:** `GET /.well-known/webfinger?resource=acct:{username}@{base-domain}` → actor id link.

**Host-meta:** `GET /.well-known/host-meta` with WebFinger template link.

**MVP outbox scope:** **Create/Update/Delete** for posts on the user's **main blog** (`Blog.main = true`). Secondary blog posts: **FQ2** in feature doc (defer or include with `attributedTo` + blog name in summary).

**Deferred on same actor (not MVP):** federated **post text highlights**, **comments**, Likes, Announce — require separate feature/ADR when implemented; actor model does not change.

**Opt-in:** `ActivityPubActor` (or equivalent) keyed **`user_id`**, `federationEnabled`; disabled users return **404** on actor endpoints (no partial leak).

### Consequences

* Pro: Mastodon “Follow” targets the author’s primary publication stream.
* Pro: Canonical post URLs in Activities match SEO/RSS.
* Con: Secondary-only authors need main blog or explicit v2 actor model.
* Other: Manage UI copy — **Fediverse publishing** toggle in Author appearance or Manage → Account.

### Confirmation

* WebFinger lookup from Mastodon search finds actor.
* Actor JSON validates against ActivityStreams 2.0 context.
* Feature checklist **FC1** in [activitypub-integration.md](../../feature/activitypub-integration.md).

## Changelog

| Data | Evento | Detalhe |
|------|--------|---------|
| 2026-07-06 | proposed | One Person actor per author; subdomain actor id. |
| 2026-07-06 | amended | Confirmed **one Person per User** (1:1); highlights/comments federation deferred — same actor later, not MVP. |
| 2026-07-06 | accepted | Aceito pelo usuário — identidade do actor em vigor. |

## More Information

* [ADR-0006](0006-activitypub-federation.md) — federation scope
* [docs/blog-subdomain-urls.md](../blog-subdomain-urls.md)
