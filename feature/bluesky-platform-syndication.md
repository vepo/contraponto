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
| Publish / republish | New observer enqueues Bluesky delivery when blog toggle on |
| Blog manage / edit | New Bluesky section (checkbox + hint) beside Git sync |
| ActivityPub Fediverse | **Independent** — Mastodon follow ≠ Bluesky channel; both may run for same publish |
| Share actions | Unchanged — manual “Share to Bluesky” compose link remains |
| Author appearance | Unchanged — `User.blueskyUrl` remains profile link only |
| RSS / SEO | Same canonical post URL in syndication message |
| Platform admin | Optional global kill-switch (FQ9) |
| Deployment | New `%prod` env vars; docker-smoke + `docs/deployment.md` |

## Summary

Enable **readers on Bluesky** to discover **published posts** from commit-mestre by following a **single platform Bluesky account** (e.g. `@commitmestre.bsky.social`). **Authors** opt in **per blog**: when enabled, each **publish** on that blog triggers an outbound `app.bsky.feed.post` via the AT Protocol XRPC API (`com.atproto.repo.createRecord`).

**Not in scope for v1:**

- Per-author Bluesky accounts or app passwords in Author appearance
- Hosting a Personal Data Server (PDS) on `*.commit-mestre.dev`
- AT Protocol custom feeds, OAuth login, or inbound Bluesky interactions
- Native interoperability with ActivityPub (Bridgy Fed remains external)

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

### Screen: N/A — public reader surfaces

No Bluesky badge on post pages in v1. Discovery is “follow the platform account on Bluesky.”

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | New **`bluesky`** (or `atproto`) under **Integration**; observes `post` / `blog` via `PostPublishedEvent` |
| Packages | `dev.vepo.contraponto.bluesky` (TBD in Architecture) |
| API / routes | No public routes in v1 — outbound XRPC only |
| UI | Blog edit checkbox + hint; optional admin kill-switch on Platform insights (if FQ9=yes) |
| Schema | `tb_blogs.bluesky_syndication_enabled` (BOOLEAN NOT NULL DEFAULT false); `tb_bluesky_deliveries` (queue: post id, blog id, status, record uri, retries) |
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
| **Delivery failures** (rate limit, PDS down) | Async queue + retries; optional author notification (FQ10) |
| **Duplicate skeets on republish** | Define FQ5 — store AT record URI; update vs new post |
| **Outbound HTTPS** to PDS | Fixed allowlist host (`bsky.social` or configured PDS); not user-supplied URL (lower SSRF than ActivityPub fetch) |

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | **Credentials:** platform Bluesky handle + app password supplied only via **Docker / `%prod` env** (not per-author UI)? | answered | **Yes** — operator configures once per deployment |
| FQ2 | **Opt-in:** per-**blog** checkbox in blog configuration, **disabled by default**? | answered | **Yes** |
| FQ3 | **Which blogs:** main blog only (like ActivityPub) or **any blog** with toggle on? | answered | **Any blog** — toggle is per `Blog` |
| FQ4 | **Message template** (fixed, Portuguese): see below? | answered | **Yes** — platform-fixed template with placeholders |
| FQ5 | **Republish** when post already syndicated: post **new** skeet, **update** existing record, or **skip**? | open | |
| FQ6 | **Unpublish:** delete Bluesky record, leave it, or out of scope v1? | open | |
| FQ7 | Add **`app.bsky.embed.external`** link card for richer preview, or plain `text` only? | open | |
| FQ8 | **Tags** in `{{TAGS}}`: hashtag per tag (`#java`), comma-separated names, or omit when empty only? | open | |
| FQ9 | Platform **admin kill-switch** to disable all Bluesky syndication (like ActivityPub FQ5)? | open | |
| FQ10 | Notify **blog owner** in-app when Bluesky delivery fails after retries? | open | |

**Message template (FQ4):**

```text
Novo post de {{mentionHandle}}  "{{postTitle}}" {{tags}}

{{postLink}}
```

| Placeholder | Source |
|-------------|--------|
| `{{mentionHandle}}` | Config `contraponto.bluesky.mention-handle` (e.g. `@commitmestre.bsky.social`) |
| `{{postTitle}}` | Title from live publication at publish time |
| `{{tags}}` | Published snapshot tags — format per FQ8 |
| `{{postLink}}` | Canonical absolute post URL (`PostPaths` + subdomain rules) |

**Gate:** phase 3 requires blocking **FQ*n*** answered or marked `not valid`. **FQ5** and **FQ6** should be resolved before task break.

### Architecture questions (AQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | Delivery: synchronous on `PostPublishedEvent` vs **async DB queue** + scheduler? | open | Recommend **async** (parity with ActivityPub AQ1) |
| AQ2 | Package name: `bluesky` vs `atproto`? | open | |
| AQ3 | Persist **AT Protocol record URI** (`at://…`) per post for update/delete? | open | Required if FQ5/FQ6 ≠ skip/none |
| AQ4 | Session: create JWT per delivery vs cache refresh token in memory? | open | |
| AQ5 | Admin UI for kill-switch: reuse Platform insights pattern from ActivityPub? | open | Depends on FQ9 |
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
| **Layers** | `BlueskyDeliveryObserver` → `BlueskyDeliveryService` → `BlueskyXrpcClient` → `BlueskyDeliveryRepository` |
| **Events** | `@Observes(during = AFTER_SUCCESS) PostPublishedEvent` when `blog.blueskySyndicationEnabled` && platform enabled |
| **XRPC** | `com.atproto.server.createSession` (app password) → `com.atproto.repo.createRecord` (`app.bsky.feed.post`) |
| **Message** | `BlueskySyndicationMessageBuilder` applies FQ4 template + FQ8 tag rules + 300-grapheme truncation |
| **Blog flag** | `Blog.blueskySyndicationEnabled` — saved via existing blog save form |
| **Config** | `BlueskySettings` — maps env; `enabled()` false when handle/password missing |
| **Tests** | Mock XRPC server; template unit tests; WebTest toggles blog checkbox |

### HTMX component model (draft)

| Component id | Fragment route | Activator | Target/swap | Events | JS | Auth allowlist |
|--------------|----------------|-----------|-------------|--------|-----|----------------|
| `#blog-edit-form` | `GET /blogs/{id}/edit` | — | — | — | none | `@Logged` blog owner |
| Bluesky fieldset | same page (inline in `gitSyncSection` sibling partial) | Blog save `hx-post` `/forms/blogs/...` | `#blog-edit-form` or full form swap per existing blog save | — | none | blog owner |

No new custom `HtmxTriggers` events in v1.

### HTMX interaction diagram

```
Blog owner checks "Enable Bluesky syndication" → Save blog
  → POST /forms/blogs/{id} → Blog.blueskySyndicationEnabled = true

Author publishes post on that blog
  → PostPublicationService → PostPublishedEvent
  → BlueskyDeliveryObserver → enqueue delivery
  → Scheduler → XRPC createRecord → platform Bluesky timeline
```

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Platform env configures Bluesky account (prod) | FQ1 | ☐ |
| FC2 | Blog edit shows Bluesky toggle (hidden when platform disabled) | FQ2 | ☐ |
| FC3 | Publish on enabled blog creates skeet with FQ4 template | FQ4 | ☐ |
| FC4 | Toggle off → no delivery on publish | FQ2 | ☐ |
| FC5 | Republish/unpublish behaviour per FQ5/FQ6 | FQ5, FQ6 | ☐ |
| FC6 | Admin kill-switch (if FQ9=yes) | FQ9 | ☐ |
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
