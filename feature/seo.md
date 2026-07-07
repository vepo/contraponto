# SEO & crawlability

**Feature version:** 1  
**Status:** done  
**Production:** live

## Changelog

### Production baseline — 2026-07-07

**Version:** 1  
**Status:** done

**Production:** live — deployed capability

## Summary

**SEO metadata** (title, description, Open Graph, JSON-LD) for public pages via `SeoService`. **HTMX navigation** refreshes `#seo-head` OOB. **Sitemap** (`/sitemap.xml`) and **robots.txt** with private path rules. Audit script: `scripts/audit-template-seo.sh`.

## Wireframe

N/A — `components/head.html`, `components/seo-oob.html` on every public page.

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `seo` |
| Routes | `GET /components/seo`, `/sitemap.xml`, `/robots.txt` |
| Integration | All public endpoints pass `SeoMetadata` to templates |
| Tests | `SeoServiceTest`, `SeoWebTest`, `SitemapServiceTest`, `RobotsEndpointTest` |



### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Canonical URL for dual blog URLs? | answered | Subdomain form when enabled — see ADR-0011 |

## Architecture

### HTMX component model

| Component id | Route | Activator | Swap |
|--------------|-------|-----------|------|
| `#seo-head`, `#page-title` | `GET /components/seo?path=` | HTMX navigation | OOB replace |

### Architecture questions (AQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | Private paths in robots? | answered | `CrawlerPrivatePaths` — search, account, write, etc. |

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Canonical URL for dual blog URLs? | answered | Subdomain form when enabled — see ADR-0011 |

#### Feature checklist

| ID | Criterion | Done |
|----|-----------|------|
| FC1 | Meta + structured data on post/blog/home | ☑ |
| FC2 | Sitemap + robots | ☑ |
| FC3 | SEO OOB on HTMX nav | ☑ |
| FCdev | N/A (structural) | ☑ |

**Review approval:** approved 2026-07-07 — production baseline
