# ActivityPub actor identity and discovery

> **Status**: Accepted
>
> **Updated**: 2026-07-08

## Summary

Each **federation-enabled user** is represented by exactly one ActivityStreams **`Person`** actor — **1:1 with `User`**, not per blog. The actor's public identity is keyed to the user's **main blog** host and username. **Actor ID**, **WebFinger** `acct:` handle, and **preferredUsername** derive from `User.username` and the platform's **public HTTPS origin** (blog subdomain canonical host when enabled). **Secondary blogs** are not separate actors.

**Outbox / syndication (v1.4):** **Create/Update/Delete** cover **all blogs** on the same Person, with distinct secondary Create content and interleaved `publishedAt` ordering. Actor identity (1:1 User) is unchanged. Cross-ref: [ADR-0006](0006-activitypub-federation.md) MVP table may still say “main blog posts in outbox” editorially — **this ADR** governs outbox membership for implementation (see feature AQ27).

**Future (out of scope for first multi-blog timeline release):** the same **User** actor may later emit activities for **post text highlights**, **comments**, and other engagement types — one identity, multiple activity kinds.

## Drivers

* Mastodon users search **`@username@domain`** via WebFinger; actor `id` must be stable and HTTPS.
* Contraponto already distinguishes **platform host** vs **blog subdomain** ([domain-spec — Dual blog URL](../domain-specification.md)).
* One **user** may own multiple blogs; Fediverse users expect **one person, one follow** for that human — not one actor per blog.
* Reader **highlights** and **comments** are natural future activity types on the **same User actor**; defer to a later ADR/feature (not MVP).
* `User.mastodonUrl` today is only a **link-out**; federation requires **`sameAs`** optional link when the user also has a remote Mastodon account.

## Options

### One Person actor per user (multi-blog outbox on same Person)

Actor ID: `https://{username}.{base-domain}/` — one actor per **`User`**; outbox / backfill / live Create cover **all blogs** owned by the user (interleaved by publication date); secondary Creates use distinct content (title + blog name + link); same actor may later carry highlights/comments activities.

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
* Con: Secondary posts share the Person follow graph; content must name the blog so remotes can distinguish streams (**FQ20**).

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

**Outbox / delivery scope (v1.4):** **Create** / **Delete** (and Update if later added) for **published** posts on **any blog** owned by the user when federation is opted in — including **live** enqueue on secondary publish/unpublish (**FQ24**; Delete mirrors Create). Same Person `attributedTo` / `actor` for all blogs (**FQ22**). Archive membership for **outbox** and **Accept/re-Follow backfill** is the **same** full published set (no recent-N; drafts/unlisted excluded as for public reading) (**FQ14**, **FQ16**, **FQ17**). Primary sort key: **`publishedAt`** interleaved across blogs (**FQ23**); outbox pages newest-first (`DESC`); backfill enqueue oldest-first (`ASC`) so remote homes fill chronologically — same membership and activity payloads. Create activity top-level **`published`** = `Post.publishedAt` (**FQ15**). **Fediverse Create content:** main = **title + canonical post link** (FQ3); secondary = **title + blog name (`Blog.name`) + canonical post link** (**FQ20**) — no post description/`summary` paragraph in `content`. Object `id` / `url` / content link = platform HTTPS via `PostPaths` / `ActivityPubPaths.postObjectId` — main `/{username}/post/{slug}`, secondary `/{username}/{blogSlug}/post/{slug}` (**FQ21**). Opt-in covers **all blogs** under one control (**FQ25**).

**Deferred on same actor:** federated **post text highlights**, **comments**, Likes, Announce — require separate feature/ADR when implemented; actor model does not change.

**Opt-in:** `ActivityPubActor` (or equivalent) keyed **`user_id`**, `federationEnabled`; disabled users return **404** on actor endpoints (no partial leak). Appearance copy must state that enabling Fediverse publishes **all blogs** (intro + checkbox description i18n).

### Consequences

* Pro: Mastodon “Follow” targets the author as one Person with a complete archive across blogs.
* Pro: Canonical platform post URLs in Activities match SEO/RSS (`PostPaths`).
* Con: Larger outbox/backfill volume for multi-blog authors; queue must tolerate full-archive Accept delivery; secondary live Create increases steady-state fan-out.
* Other: Manage UI copy — **Fediverse publishing** toggle (all blogs) in Author appearance; existing opt-ins gain secondary syndication when shipped (communicate via copy).

### Confirmation

* WebFinger lookup from Mastodon search finds actor.
* Actor JSON validates against ActivityStreams 2.0 context.
* Outbox crawl and Accept backfill list the same multi-blog archive with activity `published` and FQ20/FQ21 content/URLs.
* Feature checklist **FC1** plus v1.4 **FC25–FC33** in [activitypub-integration.md](../../feature/activitypub-integration.md).

## Changelog

| Data | Evento | Detalhe |
|------|--------|---------|
| 2026-07-06 | proposed | One Person actor per author; subdomain actor id. |
| 2026-07-06 | amended | Confirmed **one Person per User** (1:1); highlights/comments federation deferred — same actor later, not MVP. |
| 2026-07-06 | accepted | Aceito pelo usuário — identidade do actor em vigor. |
| 2026-07-08 | reopened | Usuário (FQ26): expandir outbox/syndication para todos os blogs no mesmo Person; identidade 1:1 User inalterada. |
| 2026-07-08 | amended | Architect: multi-blog outbox/backfill/live Create+Delete; Create `published`; FQ20 content; FQ21 platform URLs; ASC/DESC sort note. Aguarda re-aceite. |
| 2026-07-08 | accepted | Aceite manual do usuário — “Aceito o ADR-0008”. Multi-blog outbox/syndication em vigor. |

## More Information

* [ADR-0006](0006-activitypub-federation.md) — federation scope
* [docs/blog-subdomain-urls.md](../blog-subdomain-urls.md)
