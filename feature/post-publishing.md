# Post publishing & version history

**Feature version:** 2  
**Status:** planned  
**Production:** live (v1 — draft/publish/republish/unpublish); v2 (scheduled publishing) not shipped

## Changelog

### Scheduled publishing — 2026-07-20

**Version:** 2  
**Status:** planned

**Description:** Authors set a future date/time on a draft so it **publishes automatically** without a manual click — same publish semantics as today (immutable snapshot per [ADR-0012](../docs/adr/0012-post-publication-versioning.md), `PostPublishedEvent`, notifications, Git export, ActivityPub delivery). A background poller (same pattern as `ActivityPubDeliveryScheduler` / `GitRemotePollScheduler`) publishes due posts. Authors can reschedule, cancel (revert to draft), or publish immediately before it fires. Until it fires, a scheduled post stays invisible to readers, search, RSS, sitemap, and ActivityPub — identical visibility to an ordinary draft.

**Domain model:** N/A yet — pending phase 1b (Domain Model agent) once blocking FQs are answered.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| [multi-blog.md](multi-blog.md) | None — same author/blog ownership and `BlogAccess.canEdit` gate as manual publish |
| [blog-audience.md](blog-audience.md) | Follower notifications must fire at **actual publish time**, not at schedule-set time |
| [git-sync.md](git-sync.md) | Export-on-publish (`PostGitSyncRequestedEvent`) must fire at actual publish time, not at schedule-set time |
| [activitypub-integration.md](activitypub-integration.md) | `Create` activity delivery must fire at actual publish time; a scheduled-not-yet-due post must not appear in the outbox or be fetchable |
| [search.md](search.md) | Scheduled posts excluded from the index until published |
| [seo.md](seo.md) | Scheduled posts excluded from sitemap/crawlable links until published |
| [rss-syndication.md](rss-syndication.md) | Scheduled posts excluded from the feed until published |
| [editor-review.md](editor-review.md) | Open — can a not-yet-published scheduled post be pre-marked featured? (FQ5) |
| [dashboard-analytics.md](dashboard-analytics.md) | Possible: surface upcoming scheduled posts — open (FQ9) |
| `dev-import.sql` | Seed at least one scheduled-but-not-due post for the happy path |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC5 | Write editor offers **Schedule** alongside Save draft / Publish, with a future date+time picker | FQ3 | ☐ |
| FC6 | Scheduled post auto-publishes at the target time with identical side effects to manual publish (snapshot, notifications, Git export, ActivityPub delivery) | — | ☐ |
| FC7 | Author can reschedule, cancel (revert to draft), or publish immediately before the scheduled time fires | FQ7, FQ8 | ☐ |
| FC8 | Writing library shows a distinct **Scheduled** view/tab (not merged into Drafts or Published) | — | ☐ |
| FC9 | A due-but-missed schedule (server downtime) publishes on the next poller tick rather than silently dropping | FQ4 | ☐ |
| FC10 | Scheduled-not-yet-due posts stay invisible to readers, search, sitemap/RSS, and ActivityPub outbox — same visibility as a draft | — | ☐ |
| FCdev | `dev-import.sql` includes a scheduled-but-not-due post | — | ☐ |

#### Wireframe (v2 delta)

##### Screen: Write editor (`GET /write`, `/write/draft/{id}`)

| Region | Elements | Notes |
|--------|----------|-------|
| Toolbar | New **Schedule** action next to Save draft / Publish | Opens date/time picker (FQ3 timezone) |
| Scheduled banner | "Scheduled to publish {datetime}" + **Reschedule** / **Cancel** / **Publish now** | Shown when the open draft has a pending schedule |

##### Screen: Writing library (`GET /writing/library`)

| Region | Elements | Notes |
|--------|----------|-------|
| Tabs | New **Scheduled** tab alongside Drafts / Published | Row shows target date/time; row actions: Reschedule, Cancel, Publish now |

### Production baseline — 2026-07-07

**Version:** 1  
**Status:** done

**Production:** live — deployed capability

## Summary

Authors write posts in the **editor** (`/write`), **save drafts**, **publish** immutable **publication snapshots** ([ADR-0012](../docs/adr/0012-post-publication-versioning.md)), **republish** when the working copy diverges, **unpublish**, and manage posts in the **Writing hub library**. Readers see the **live publication** on the post page; authors view **version history** and diffs. Publish/unpublish fires CDI events for downstream contexts ([ADR-0013](../docs/adr/0013-cdi-events-cross-context.md)).

**v2** (planned) adds **scheduled publishing**: authors set a future date/time on a draft and a background poller publishes it automatically with the same side effects as a manual publish. See the changelog entry above for scope and open questions.

## Wireframe

| Screen | Route | Notes |
|--------|-------|-------|
| Write editor | `GET /write`, `/write/draft/{id}` | Toolbar: Save draft, Publish; **v2:** Schedule |
| Post (reader) | `GET /{username}/post/{slug}` | Version control, Edit (author) |
| Version history modal | `GET …/components/history/modal` | Diff between snapshots |
| Writing library | `GET /writing/library` | Drafts / Published tabs; **v2:** Scheduled tab |

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `post`, `write`, `library` |
| Schema (v1) | `tb_posts`, `tb_post_publications`, `tb_post_slug_aliases`, tag/image dependency tables |
| Schema (v2) | Scheduled-publish target time on `tb_posts` (or a dedicated pending-schedule table, mirroring `tb_git_sync_runs`/ActivityPub delivery-queue patterns) — exact shape is an Architect decision (phase 2) |
| CDI (v1) | `PostPublishedEvent`, `PostUnpublishedEvent`, `PostGitSyncRequestedEvent` |
| CDI (v2) | Same events, fired by the scheduler at actual publish time instead of a user request |
| Tests | `PostPublicationServiceTest`, `PublishEndpointTest`, `WriteTest`, `PostChangeHistoryTest`, `LibraryEndpoint` tests; **v2:** scheduler poll/catch-up, pre-publish visibility (search/RSS/ActivityPub) |

### Risks

| Risk | Mitigation |
|------|------------|
| Identical republish spam | `isIdenticalSnapshot` skips new version + notifications |
| Published post delete | Must unpublish first |
| **(v2)** Missed schedule (server downtime spans the target time) | Poller catch-up publishes late on the next tick instead of silently dropping (FQ4) |
| **(v2)** Timezone ambiguity between author and server | Resolve via FQ3 before architecture proceeds |
| **(v2)** Scheduled-not-yet-due post leaks early via search/RSS/ActivityPub | Reuse the existing `published=false` visibility gate — no new read path bypasses it (FC10) |

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Delete publication snapshots on unpublish? | answered | **No** — retained for history |
| FQ2 | Featured curation in publish flow? | answered | **No** — separate editor-review feature |
| FQ3 | What timezone governs the scheduled time — UTC/server, or the author's browser-local time converted on save? | open | |
| FQ4 | If the scheduled time already passed before the poller catches up (e.g. server was down), publish immediately on the next tick, or require the author to notice and act? | open | |
| FQ5 | Can a scheduled post be marked **featured** (editor-review) before it goes live, so it's already featured the instant it publishes? | open | |
| FQ6 | Does the author get an in-app notification confirming the scheduled publish succeeded or failed (e.g. a slug conflict introduced meanwhile)? | open | |
| FQ7 | Minimum lead time — can an author schedule "5 minutes from now," or is there a floor tied to the poller interval? | open | |
| FQ8 | Does Cancel/reschedule use the existing confirm-modal pattern ([contraponto-confirm-modals.mdc](../.cursor/rules/contraponto-confirm-modals.mdc)), same as unpublish/delete? | open | |
| FQ9 | Should the dashboard surface an author's own upcoming scheduled posts, or is the Writing library's Scheduled tab sufficient? | open | |

**Blocking for architecture:** FQ3, FQ4 — timezone and catch-up semantics shape the schema and scheduler design. FQ5–FQ9 informational, can close during phase 2.

## Architecture

### ADRs aplicáveis

| ADR | Relevância |
|-----|------------|
| [0002](../docs/adr/0002-backend-java-quarkus-jakarta-ee.md) | Backend |
| [0003](../docs/adr/0003-frontend-qute-htmx.md) | Write + library HTMX |
| [0012](../docs/adr/0012-post-publication-versioning.md) | Snapshots |
| [0013](../docs/adr/0013-cdi-events-cross-context.md) | Publish side effects |

### Design específico da feature

| Area | Design |
|------|--------|
| Services | `PostPublicationService.publish`, `PostManagementService.unpublish/delete`, `PostChangeDiffService` |
| Access | `PostAccess` → `BlogAccess.canEdit` |
| Write | `PostWriteService` IDOR-safe resolution |
| Images | `PostImageDependencyService` sync on draft; snapshot on publish |

### HTMX component model

| Component id | Route | Activator | Target/swap | Events out | Events in | JS |
|--------------|-------|-----------|-------------|------------|-----------|-----|
| Write save | `POST /forms/write/draft` | Save button | `none` (toast URL) | toast | — | `write.js` dirty state |
| Write publish | `POST /forms/write/publish` | Publish | `main` from post page | toast | — | `write.js` |
| Library tabs | `GET /writing/library/components/tab/{type}` | Tab click | `#libraryContent` | — | — | none |
| Unpublish/delete | confirm modal → forms | Row action | row or hub | toast | — | confirm modal |
| History modal | `GET …/components/history/modal` | Version button | `#modal-container` | — | — | none |

### Architecture questions (AQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | Publish response swaps full post page? | answered | **Yes** — `hx-target="main"` |

#### Feature checklist

| ID | Criterion | Done |
|----|-----------|------|
| FC1 | Save draft / publish / republish | ☑ |
| FC2 | Unpublish + delete draft | ☑ |
| FC3 | Writing library tabs | ☑ |
| FC4 | Version history modal | ☑ |
| FCdev | Draft + multi-version published posts in seed | ☑ |

**Development approval:** approved — production baseline (shipped)
**Review approval:** approved 2026-07-07 — production baseline
