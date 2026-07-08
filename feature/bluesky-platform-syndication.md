# Bluesky platform syndication

**Feature version:** 1  
**Status:** planned  
**Production:** not deployed

## Changelog

### Bluesky platform syndication — 2026-07-07

**Version:** 1  
**Status:** planned

**Description:** When an author **publishes** on a blog with Bluesky syndication enabled, Contraponto posts a skeet to the **platform-owned Bluesky account** using credentials from production environment variables. Each blog opts in via **blog configuration** (disabled by default). Bluesky users follow the platform account to see commit-mestre posts in their home timeline.

**Domain model:** pending phase 1b — extend ubiquitous language (Bluesky syndication, syndication message).

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Publish / republish | Create or update Bluesky record when blog toggle on; republish updates only if **title changed** (FQ5) |
| Unpublish | Delete Bluesky record when post unpublished (FQ6) |
| Blog manage / edit | New Bluesky section (checkbox + hint) beside Git sync |
| Notifications | New type `BLUESKY_SYNDICATION_FAILED` → blog owner after exhausted retries (FQ10) |
| ActivityPub Fediverse | **Independent** — Mastodon follow ≠ Bluesky channel; both may run for same publish |
| Share actions | Unchanged — manual “Share to Bluesky” compose link remains |
| Author appearance | Unchanged — `User.blueskyUrl` remains profile link only |
| RSS / SEO | Same canonical post URL in syndication message + link-card embed (FQ7) |
| Platform admin | **Required** global kill-switch on admin hub (FQ9) — mirror ActivityPub platform panel |
| Deployment | New `%prod` env vars; docker-smoke + `docs/deployment.md` |

## Summary

Enable **readers on Bluesky** to discover **published posts** from commit-mestre by following a **single platform Bluesky account** (e.g. `@commitmestre.bsky.social`). **Authors** opt in **per blog**: when enabled, each **publish** on that blog triggers an outbound `app.bsky.feed.post` via the AT Protocol XRPC API (`com.atproto.repo.createRecord`).

**Lifecycle (FQ5–FQ7):**

- **First publish:** `createRecord` with `text` (FQ4 template), `app.bsky.embed.external` link card (FQ7); store `at://` record URI + last syndicated title.
- **Republish:** if title **unchanged** → no Bluesky action. If title **changed** → `putRecord` update; on failure → `deleteRecord` then `createRecord`.
- **Unpublish:** `deleteRecord` for stored URI (FQ6).

**Not in scope for v1:**

- Per-author Bluesky accounts or app passwords in Author appearance
- Hosting a Personal Data Server (PDS) on `*.commit-mestre.dev`
- AT Protocol custom feeds, OAuth login, or inbound Bluesky interactions
- Native interoperability with ActivityPub (Bridgy Fed remains external)
- Republish Bluesky update when only body/tags/link change but title unchanged

**Depends on (Accepted ADRs):**

- [ADR-0002](../docs/adr/0002-backend-java-quarkus-jakarta-ee.md) — Quarkus HTTP client for XRPC  
- [ADR-0003](../docs/adr/0003-frontend-qute-htmx.md) — Blog settings UI  
- [ADR-0005](../docs/adr/0005-postgresql-database.md) — Delivery queue + blog flag  
- [ADR-0013](../docs/adr/0013-cdi-events-cross-context.md) — `PostPublishedEvent` observer pattern  

**New ADR (phase 2):** likely [ADR-0016](../docs/adr/0016-bluesky-platform-syndication.md) — platform credentials, outbound XRPC, message template (Architect draft → `Proposed`).

## Wireframe

| Field | Value |
|-------|--------|
| **Source** | ASCII below |
| **Last updated** | 2026-07-07 |

### Screen: Blog edit — Bluesky syndication section

Placed in blog manage/edit form alongside **Git sync** (`BlogManageEndpoint` / `gitSyncSection.html` pattern). Shown only when platform Bluesky is configured (`contraponto.bluesky.enabled` + credentials present); otherwise hidden or disabled with operator hint.

```
┌─ Bluesky (platform channel) ──────────────────────────────┐
│ Publish new posts to the Commit Mestre Bluesky account    │
│ when I publish on this blog.                              │
│                                                           │
│ [ ] Enable Bluesky syndication for this blog              │
│     (off by default)                                      │
│                                                           │
│ Follow us on Bluesky: @commitmestre.bsky.social         │
│ (read-only link — handle from platform config)            │
│                                                           │
│ Posts use the platform message format (title, tags, link).│
└───────────────────────────────────────────────────────────┘
```

### Screen: N/A — post editor

No change. Publish triggers syndication automatically when blog toggle is on (same as Git export on publish).

### Screen: Platform admin — Bluesky syndication kill-switch

Mirror [ActivityPubPlatformManageEndpoint/panel.html](../src/main/resources/templates/ActivityPubPlatformManageEndpoint/panel.html) on Platform insights / administration hub.

```
┌─ Bluesky (platform syndication) ──────────────────────────┐
│ [ ] Allow Bluesky syndication for opted-in blogs          │
│     Turn off to halt all outbound Bluesky posts           │
│     immediately (credentials remain in env).              │
│                                                           │
│ [ Save Bluesky settings ]                                 │
└───────────────────────────────────────────────────────────┘
```

### Screen: N/A — public reader surfaces

No Bluesky badge on post pages in v1. Discovery is “follow the platform account on Bluesky.”

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | New **`bluesky`** under **Integration**; observes `post` / `blog`; notifies via `notification` on failure |
| Packages | `dev.vepo.contraponto.bluesky` (TBD in Architecture) |
| API / routes | Admin form `POST /forms/administration/bluesky` (kill-switch); outbound XRPC only otherwise |
| UI | Blog edit checkbox + hint; admin kill-switch panel (FQ9) |
| Schema | `tb_blogs.bluesky_syndication_enabled`; `tb_bluesky_platform_settings` (singleton kill-switch); `tb_bluesky_post_records` (post id → `at_uri`, `last_syndicated_title`); `tb_bluesky_deliveries` (queue + retries) |
| Notifications | `NotificationType.BLUESKY_SYNDICATION_FAILED` + `NotificationService` helper (FQ10) |
| Config | `%prod`: `contraponto.bluesky.enabled`, `.handle`, `.app-password`, `.mention-handle`, `.pds-url` (default `https://bsky.social`) |
| **`dev-import.sql`** | One blog with toggle on for manual dev (e.g. `vepo` secondary or `alice` main) when `%dev` credentials set |
| Tests | Unit (message template, truncation); integration (mock XRPC); `@WebTest` blog settings toggle |
| Docs | domain-spec, feature-catalog, deployment.md, docker-smoke README, ARCHITECTURE § syndication |

### Risks

| Risk | Mitigation |
|------|------------|
| **App password leak** via env misconfiguration | Document in deployment.md; never log password; use Bluesky app password only |
| **Platform account spam / reputation** | Per-blog opt-in; author owns blog content; operator moderates platform account |
| **300-character limit** on Bluesky | Truncate title/tags in template builder; always preserve canonical URL |
| **Delivery failures** (rate limit, PDS down) | Async queue + retries; in-app notification to blog owner (FQ10) |
| **Republish / update failure** | FQ5 fallback: delete old record then create new; log + notify if still failing |
| **Delete failure on unpublish** | Retry queue; notify owner if record cannot be removed (stale skeet may remain) |
| **Outbound HTTPS** to PDS | Fixed allowlist host (`bsky.social` or configured PDS); not user-supplied URL (lower SSRF than ActivityPub fetch) |

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | **Credentials:** platform Bluesky handle + app password supplied only via **Docker / `%prod` env** (not per-author UI)? | answered | **Yes** — operator configures once per deployment |
| FQ2 | **Opt-in:** per-**blog** checkbox in blog configuration, **disabled by default**? | answered | **Yes** |
| FQ3 | **Which blogs:** main blog only (like ActivityPub) or **any blog** with toggle on? | answered | **Any blog** — toggle is per `Blog` |
| FQ4 | **Message template** (fixed, Portuguese): see below? | answered | **Yes** — platform-fixed template with placeholders |
| FQ5 | **Republish** when post already syndicated: post **new** skeet, **update** existing record, or **skip**? | answered | **Update** existing record when **title changed**; if update fails, **delete** old record then **create** new. If title unchanged, **skip** Bluesky action. |
| FQ6 | **Unpublish:** delete Bluesky record, leave it, or out of scope v1? | answered | **Delete** Bluesky record (`deleteRecord`) |
| FQ7 | Add **`app.bsky.embed.external`** link card for richer preview, or plain `text` only? | answered | **Yes** — link card embed on create and update |
| FQ8 | **Tags** in `{{TAGS}}`: hashtag per tag (`#java`), comma-separated names, or omit when empty only? | answered | **`#PascalCase` per tag**, space-separated (e.g. `#Java #DistributedSystems`); omit `{{tags}}` segment when no tags |
| FQ9 | Platform **admin kill-switch** to disable all Bluesky syndication (like ActivityPub FQ5)? | answered | **Yes** — admin hub panel; halts all outbound syndication |
| FQ10 | Notify **blog owner** in-app when Bluesky delivery fails after retries? | answered | **Yes** — after delivery queue exhausts retries |

**Message template (FQ4):**

```text
Novo post de {{mentionHandle}}  "{{postTitle}}" {{tags}}

{{postLink}}
```

| Placeholder | Source |
|-------------|--------|
| `{{mentionHandle}}` | Config `contraponto.bluesky.mention-handle` (e.g. `@commitmestre.bsky.social`) |
| `{{postTitle}}` | Title from live publication at publish time |
| `{{tags}}` | Published snapshot tags — FQ8: `#PascalCase` each, space-separated |
| `{{postLink}}` | Canonical absolute post URL (`PostPaths` + subdomain rules) |

**Tag → hashtag rule (FQ8):** For each tag on the live publication, render `#` + PascalCase of the tag **name** (split on spaces, hyphens, underscores; capitalize each segment; concatenate — e.g. `distributed-systems` → `#DistributedSystems`, `java` → `#Java`). Join with a single space. Truncate from the end if needed for the 300-grapheme limit (after preserving URL).

**Gate:** phase 3 requires blocking **FQ*n*** answered or marked `not valid`. **All FQ*n*** answered — ready for phase 1b / 2.

### Architecture questions (AQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | Delivery: synchronous on `PostPublishedEvent` vs **async DB queue** + scheduler? | open | Recommend **async** (parity with ActivityPub AQ1) |
| AQ2 | Package name: `bluesky` vs `atproto`? | open | |
| AQ3 | Persist **AT Protocol record URI** (`at://…`) per post for update/delete? | answered | **Yes** — `tb_bluesky_post_records` with `at_uri` + `last_syndicated_title` (FQ5/FQ6) |
| AQ4 | Session: create JWT per delivery vs cache refresh token in memory? | open | |
| AQ5 | Admin UI for kill-switch: reuse Platform insights pattern from ActivityPub? | answered | **Yes** — `BlueskyPlatformManageEndpoint` mirrors ActivityPub platform panel (FQ9) |
| AQ6 | New ADR for platform syndication scope + credential handling? | open | Recommend **yes** — ADR-0016 |

## Architecture

> **Phase 2 — Architect Agent.** Fill ADR-0016, HTMX model, and tables below; set status `architecture-ready`. No production code until phases 1–4 complete.

### ADRs aplicáveis

| ADR | Status | Relevância |
|-----|--------|------------|
| [0002](../docs/adr/0002-backend-java-quarkus-jakarta-ee.md) | Accepted | REST client, config mapping |
| [0003](../docs/adr/0003-frontend-qute-htmx.md) | Accepted | Blog settings form |
| [0005](../docs/adr/0005-postgresql-database.md) | Accepted | Flyway + queue table |
| [0013](../docs/adr/0013-cdi-events-cross-context.md) | Accepted | `PostPublishedEvent` observer |
| [0016](../docs/adr/0016-bluesky-platform-syndication.md) | Proposed (TBD) | Platform AT Protocol outbound scope |

### Design específico da feature (draft)

| Area | Design (draft — confirm in phase 2) |
|------|--------------------------------------|
| **Bounded context** | `bluesky` — Integration; depends on `shared`, `blog`, `post` |
| **Layers** | `BlueskyDeliveryObserver`, `BlueskyUnpublishObserver` → `BlueskyDeliveryService` → `BlueskyXrpcClient` → `*Repository`; `BlueskyPlatformSettingsEndpoint` (admin) |
| **Events** | `PostPublishedEvent` → create/update queue; `PostUnpublishedEvent` → delete queue (FQ6) |
| **XRPC** | `createSession` → `createRecord` / `putRecord` / `deleteRecord`; embed `app.bsky.embed.external` (FQ7) |
| **Message** | `BlueskySyndicationMessageBuilder` — FQ4 template + FQ8 hashtags + 300-grapheme truncation |
| **Republish** | Compare title to `last_syndicated_title`; update or delete+create per FQ5 |
| **Failure notify** | `NotificationService.notifyBlueskySyndicationFailed(blog, post)` after max retries (FQ10) |
| **Blog flag** | `Blog.blueskySyndicationEnabled` — saved via existing blog save form |
| **Config** | `BlueskySettings` — maps env; `enabled()` false when handle/password missing |
| **Tests** | Mock XRPC server; template unit tests; WebTest toggles blog checkbox |

### HTMX component model (draft)

| Component id | Fragment route | Activator | Target/swap | Events | JS | Auth allowlist |
|--------------|----------------|-----------|-------------|--------|-----|----------------|
| `#blog-edit-form` | `GET /blogs/{id}/edit` | — | — | — | none | `@Logged` blog owner |
| Bluesky fieldset | same page (inline in `gitSyncSection` sibling partial) | Blog save `hx-post` `/forms/blogs/...` | `#blog-edit-form` or full form swap per existing blog save | — | none | blog owner |
| Admin kill-switch | `GET` platform insights Bluesky panel | Admin `hx-post` `/forms/administration/bluesky` | panel region or hub swap per ActivityPub pattern | — | none | `ADMIN` |

No new custom `HtmxTriggers` events in v1.

### HTMX interaction diagram

```
Blog owner checks "Enable Bluesky syndication" → Save blog
  → POST /forms/blogs/{id} → Blog.blueskySyndicationEnabled = true

Author publishes post on that blog
  → PostPublicationService → PostPublishedEvent
  → BlueskyDeliveryObserver → enqueue delivery
  → Scheduler → XRPC createRecord (or putRecord / delete+create on republish)
  → platform Bluesky timeline

Author unpublishes
  → PostUnpublishedEvent → BlueskyUnpublishObserver → deleteRecord

Delivery exhausted retries
  → NotificationService → blog owner in-app notification
```

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Platform env configures Bluesky account (prod) | FQ1 | ☐ |
| FC2 | Blog edit shows Bluesky toggle (hidden when platform disabled) | FQ2 | ☐ |
| FC3 | Publish on enabled blog creates skeet with FQ4 template | FQ4 | ☐ |
| FC4 | Toggle off → no delivery on publish | FQ2 | ☐ |
| FC5 | Republish: update when title changes; delete+create fallback; skip when title unchanged | FQ5 | ☐ |
| FC6 | Unpublish deletes Bluesky record | FQ6 | ☐ |
| FC7 | Link card embed on skeet | FQ7 | ☐ |
| FC8 | Hashtags `#PascalCase` space-separated in message | FQ8 | ☐ |
| FC9 | Admin kill-switch halts all syndication | FQ9 | ☐ |
| FC10 | Blog owner notified after delivery failure | FQ10 | ☐ |
| FCdev | `dev-import.sql` — at least one blog with toggle on for manual click-through | dev-import | ☐ |

#### Tasks (phase 3)

_To be filled by Task Modeller after architecture-ready and FQ gate._

#### Test coverage (phase 3)

_To be filled by Task Modeller._

## Related capabilities

| Capability | Relationship |
|------------|--------------|
| [activitypub-integration.md](activitypub-integration.md) | Parallel syndication — Fediverse vs Bluesky channel |
| [git-sync.md](git-sync.md) | Same blog-settings pattern (`git_enabled` fieldset) |
| [rss-syndication.md](rss-syndication.md) | Same publish event; RSS is pull, Bluesky is push |
| [author-profile.md](author-profile.md) | `blueskyUrl` profile link unchanged |

## Operator notes (commit-mestre)

Example production env (names TBD in Architecture / `application.properties`):

```bash
CONTRAPONTO_BLUESKY_ENABLED=true
CONTRAPONTO_BLUESKY_HANDLE=commitmestre.bsky.social
CONTRAPONTO_BLUESKY_APP_PASSWORD=<app-password-from-bsky-settings>
CONTRAPONTO_BLUESKY_MENTION_HANDLE=@commitmestre.bsky.social
CONTRAPONTO_BLUESKY_PDS_URL=https://bsky.social
```

Bluesky users discover posts by **following the platform account**; blog authors enable syndication per blog they own.
