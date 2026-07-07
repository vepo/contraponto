# Git ↔ Jekyll sync

**Feature version:** 1  
**Status:** done  
**Production:** live

## Changelog

### Production baseline — 2026-07-07

**Version:** 1  
**Status:** done

**Production:** live — deployed capability

## Summary

Per-blog **Git integration** exports published posts (and draft saves) to a remote **Jekyll-compatible** repository and **imports** changes on schedule or blog-save warmup. **Sync history** UI shows run log and detail. Failures notify via in-app notifications.

## Wireframe

| Screen | Route | Notes |
|--------|-------|-------|
| Blog edit — Git section | `GET /blogs/{id}/edit` | Enable, remote URL, branch |
| Sync history | `GET /blogs/{blogId}/git-sync` | Paginated runs |
| Run detail | `GET /blogs/{blogId}/git-sync/{runId}` | Log entries |

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `git` (integration) |
| Schema | `tb_blogs` git columns; `tb_git_sync_runs`, `tb_git_sync_run_entries` |
| CDI | `PostGitSyncRequestedEvent` → `GitPostCommittedObserver` |
| Docs | [git-jekyll-convention.md](../docs/git-jekyll-convention.md) |
| Tests | `GitSyncHistoryTest`, `BlogGitIntegrationServiceTest`, `BlogSaveEndpointTest` |



### Risks

| Risk | Mitigation |
|------|------------|
| Remote credentials in URL/env | Not in UI; operator config |
| Poll disabled in prod default | `%prod.contraponto.git.poll-enabled=false` |
| Import success silent | Only export success notifies (FQ2) |

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Manual "Sync now" button? | open | **Not implemented** — triggers only via publish/draft/poll/warmup |
| FQ2 | Notify on import success? | answered | **No** — failures/partials + export success only |

## Architecture

### ADRs aplicáveis

| ADR | Relevância |
|-----|------------|
| [0013](../docs/adr/0013-cdi-events-cross-context.md) | `PostGitSyncRequestedEvent` |

### Design específico da feature

| Area | Design |
|------|--------|
| Orchestration | `BlogGitIntegrationService`, `BlogGitImportService` |
| Runs | `GitSyncRunService`, `GitSyncRunTransaction` + notification policy |
| Triggers | `PUBLISH`, `DRAFT_SAVE`, `REMOTE_POLL`, `BLOG_SAVE_WARMUP` |
| Convention | Jekyll front matter — `docs/git-jekyll-convention.md` |

### HTMX component model

| Component | Pattern |
|-----------|---------|
| History list/detail | Full pages; `data-hx-get` nav with `hx-select="main"` |
| Blog Git fields | Static form in `BlogManageEndpoint` |
| Notifications | Link to run detail via `NotificationType` git sync link |

### Architecture questions (AQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | Async export after publish? | answered | **Yes** — observer `AFTER_SUCCESS` |

#### Feature checklist

| ID | Criterion | Done |
|----|-----------|------|
| FC1 | Enable Git on blog settings | ☑ |
| FC2 | Export on publish/draft | ☑ |
| FC3 | Sync history + run detail UI | ☑ |
| FC4 | Failed sync notifications | ☑ |
| FCdev | `alice`, `vepo` blogs with git_enabled | ☑ |

**Development approval:** approved — production baseline (shipped)
**Review approval:** approved 2026-07-07 — production baseline
