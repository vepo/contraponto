# Post publishing & version history

**Feature version:** 1  
**Status:** done  
**Production:** live

## Changelog

### Production baseline — 2026-07-07

**Version:** 1  
**Status:** done

**Production:** live — deployed capability

## Summary

Authors write posts in the **editor** (`/write`), **save drafts**, **publish** immutable **publication snapshots** ([ADR-0012](../docs/adr/0012-post-publication-versioning.md)), **republish** when the working copy diverges, **unpublish**, and manage posts in the **Writing hub library**. Readers see the **live publication** on the post page; authors view **version history** and diffs. Publish/unpublish fires CDI events for downstream contexts ([ADR-0013](../docs/adr/0013-cdi-events-cross-context.md)).

## Wireframe

| Screen | Route | Notes |
|--------|-------|-------|
| Write editor | `GET /write`, `/write/draft/{id}` | Toolbar: Save draft, Publish |
| Post (reader) | `GET /{username}/post/{slug}` | Version control, Edit (author) |
| Version history modal | `GET …/components/history/modal` | Diff between snapshots |
| Writing library | `GET /writing/library` | Drafts / Published tabs |

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `post`, `write`, `library` |
| Schema | `tb_posts`, `tb_post_publications`, `tb_post_slug_aliases`, tag/image dependency tables |
| CDI | `PostPublishedEvent`, `PostUnpublishedEvent`, `PostGitSyncRequestedEvent` |
| Tests | `PostPublicationServiceTest`, `PublishEndpointTest`, `WriteTest`, `PostChangeHistoryTest`, `LibraryEndpoint` tests |



### Risks

| Risk | Mitigation |
|------|------------|
| Identical republish spam | `isIdenticalSnapshot` skips new version + notifications |
| Published post delete | Must unpublish first |

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Delete publication snapshots on unpublish? | answered | **No** — retained for history |
| FQ2 | Featured curation in publish flow? | answered | **No** — separate editor-review feature |

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
