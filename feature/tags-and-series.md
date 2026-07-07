# Tags & series

**Feature version:** 1  
**Status:** done  
**Production:** live

## Changelog

### Production baseline — 2026-07-07

**Version:** 1  
**Status:** done

**Production:** live — deployed capability

## Summary

**Tags** classify posts (`GET /tags/{slug}`) with **main authors** sidebar; **editors** edit tag metadata. **Series** group ordered posts on main or secondary blogs (`GET /{username}/serie/{slug}`). Write editor offers **tag suggestions**.

## Wireframe

| Screen | Route | Notes |
|--------|-------|-------|
| Tag page | `GET /tags/{slug}` | Grid + load more + RSS |
| Tag edit | `GET /tags/{slug}/edit` | Editor only |
| Editor tags hub | `GET /editor/tags` | Paginated admin list |
| Serie page | `GET /{username}/serie/{slug}` | Ordered post list |

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `tag`, `serie` |
| Tests | `TagPageTest`, `TagManageTest`, `SeriePageTest`, `TagServiceTest` |



### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Serie pagination? | answered | **No** — full ordered list on page |

## Architecture

### HTMX component model

| Component | Route | Activator |
|-----------|-------|-----------|
| Tag grid | `GET /tags/{slug}/components/grid` | Load more |
| Tag save | `POST /forms/tags/update` | Editor form |

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Serie pagination? | answered | **No** — full ordered list on page |

#### Feature checklist

| ID | Criterion | Done |
|----|-----------|------|
| FC1 | Public tag listing + RSS | ☑ |
| FC2 | Editor tag metadata | ☑ |
| FC3 | Serie navigation from posts | ☑ |
| FCdev | Tagged posts + series in seed | ☑ |

**Review approval:** approved 2026-07-07 — production baseline
