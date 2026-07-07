# Search

**Feature version:** 1  
**Status:** done  
**Production:** live

## Changelog

### Production baseline — 2026-07-07

**Version:** 1  
**Status:** done

**Production:** live — deployed capability

## Summary

**Quick search** modal from header icon and **full search page** (`/search?q=`). Results paginate via HTMX load more. Search surfaces are **noindex** for crawlers.

## Wireframe

| Screen | Route | Notes |
|--------|-------|-------|
| Search modal | `GET /search/modal` | Header search icon |
| Full search | `GET /search?q=` | No primary nav link |
| Results fragment | `GET /search/results` | Load more |

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `search` |
| Packages | `SearchEndpoint`, `PostRepository.search` |
| SEO | `CrawlerPrivatePaths` — `/search` noindex |
| Tests | `SearchTest` |



### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Primary nav link to full search? | answered | **No** — modal-first UX |

## Architecture

### HTMX component model

| Component | Route | Activator | Target |
|-----------|-------|-----------|--------|
| Modal | `GET /search/modal` | Header icon | `#modal-container` |
| Results | `GET /search/results` | Load more on `/search` | results region |

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Primary nav link to full search? | answered | **No** — modal-first UX |

#### Feature checklist

| ID | Criterion | Done |
|----|-----------|------|
| FC1 | Header search modal | ☑ |
| FC2 | Full search page + pagination | ☑ |
| FCdev | Enough posts to search in seed | ☑ |

**Review approval:** approved 2026-07-07 — production baseline
