# RSS syndication

**Feature version:** 1  
**Status:** done  
**Production:** live

## Changelog

### Production baseline — 2026-07-07

**Version:** 1  
**Status:** done

**Production:** live — deployed capability

## Summary

XML **RSS feeds** for site-wide, per-user, per-blog, per-serie, and per-tag discovery. Feeds cached (`rss-feeds`); invalidated on `PostPublishedEvent` / `PostUnpublishedEvent`. UI **RSS** link buttons on home, blog, tag, serie surfaces.

## Wireframe

N/A — XML endpoints + link buttons in header/aside/footer.

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `rss` |
| Routes | `/feed`, `/{username}/feed`, `/{username}/feed/main-blog`, blog/serie/tag variants |
| Packages | `RssFeedService`, `RssFeedRenderer`, `RssFeedPaths` |
| Cache | `@CacheResult("rss-feeds")`; `RssFeedCacheInvalidator` |
| Tests | `RssFeedEndpointTest`, `RssFeedLinkWebTest` |



### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Feed item limit? | answered | **50** (`RssFeedPaths.FEED_LIMIT`) |
| FQ2 | Index feeds in robots? | answered | **Disallow** `/feed` in robots.txt |

## Architecture

### ADRs aplicáveis

| ADR | Relevância |
|-----|------------|
| [0013](../docs/adr/0013-cdi-events-cross-context.md) | Cache invalidation on publish |

### Design específico

| Area | Design |
|------|--------|
| Canonical URLs | Prefer blog subdomain when enabled (`BlogPublicUrlService`) |
| Headers | `X-Robots-Tag: noindex` via `RssNoIndexFilter` |

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Feed item limit? | answered | **50** (`RssFeedPaths.FEED_LIMIT`) |
| FQ2 | Index feeds in robots? | answered | **Disallow** `/feed` in robots.txt |

#### Feature checklist

| ID | Criterion | Done |
|----|-----------|------|
| FC1 | Site + blog + tag + serie feeds | ☑ |
| FC2 | RSS buttons in UI | ☑ |
| FCdev | Published posts for feed content | ☑ |

**Review approval:** approved 2026-07-07 — production baseline
