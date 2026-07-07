# Editor review & featured curation

**Feature version:** 1  
**Status:** done  
**Production:** live

## Changelog

### Production baseline — 2026-07-07

**Version:** 1  
**Status:** done

**Production:** live — deployed capability

## Summary

**Editors** (`EDITOR` role) curate **featured posts** on the platform home via the **Review hub** (`/editor/review`) and in-context star on post pages. **Tag metadata** administration lives under Review → Tags. Distinct from author publish workflow ([post-publishing.md](post-publishing.md)).

## Wireframe

| Screen | Route | Notes |
|--------|-------|-------|
| Review hub | `GET /editor`, `/editor/review` | Featured posts panel default |
| Featured list | Paginated rows | Star toggle per post |
| Tags admin | `GET /editor/tags` | Edit metadata links |
| Post page star | `PUT …/component/featured/toggle` | Editor on any published post |

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `admin` (`ReviewEndpoint`), `tag` |
| Auth | `LoggedUser.isEditor()` |
| Tests | `ReviewTest`, `TagManageTest` |



### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Can editors publish on others' blogs? | answered | **No** — featured only; publish remains owner |

## Architecture

### HTMX component model

| Component | Route | Activator | Target |
|-----------|-------|-----------|--------|
| Featured row | `PUT /editor/review/components/{postId}/featured/toggle` | Star click | row swap |
| Post star | `PUT …/component/featured/toggle` | Action bar | toggle fragment |

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Can editors publish on others' blogs? | answered | **No** — featured only; publish remains owner |

#### Feature checklist

| ID | Criterion | Done |
|----|-----------|------|
| FC1 | Review hub featured list | ☑ |
| FC2 | Toggle featured on post page | ☑ |
| FC3 | Tag admin under Review | ☑ |
| FCdev | `editor` user + featured posts in seed | ☑ |

**Review approval:** approved 2026-07-07 — production baseline
