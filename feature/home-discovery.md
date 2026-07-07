# Home & discovery

**Feature version:** 1  
**Status:** done  
**Production:** live

## Changelog

### Production baseline — 2026-07-07

**Version:** 1  
**Status:** done

**Production:** live — deployed capability

## Summary

Platform **featured homepage** (`/`), **author directory** (`/authors`), **blog directory** (`/explore/blogs`), and **author profiles** (`/authors/{username}`). **Editors** curate featured posts (see [editor-review.md](editor-review.md)). Guests see optional editorial masthead on home.

## Wireframe

| Screen | Route | Notes |
|--------|-------|-------|
| Featured home | `GET /` | Hero + featured grid + right margin explore |
| Load more | `GET /components/home/grid` | HTMX pagination |
| Author directory | `GET /authors` | Cards with top tags |
| Blog directory | `GET /explore/blogs` | Active blogs with posts |
| Author profile | `GET /authors/{username}` | Bio, blogs, recent posts, Message button |

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `home`, `directory` |
| Packages | `HomeEndpoint`, `AuthorDirectoryEndpoint`, `BlogDirectoryEndpoint`, `AuthorProfileEndpoint` |
| Tests | `HomeTest`, `ExploreDirectoryWebTest`, `AuthorProfileEndpointTest` |



### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Guest masthead dismissible? | answered | **Yes** — session/dismiss pattern on home |

## Architecture

### ADRs aplicáveis

| ADR | Relevância |
|-----|------------|
| [0003](../docs/adr/0003-frontend-qute-htmx.md) | Home load more |

### HTMX component model

| Component id | Route | Activator | Target/swap | JS |
|--------------|-------|-----------|-------------|-----|
| Home grid | `GET /components/home/grid` | Load more | append grid | none |

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Guest masthead dismissible? | answered | **Yes** — session/dismiss pattern on home |

#### Feature checklist

| ID | Criterion | Done |
|----|-----------|------|
| FC1 | Featured homepage | ☑ |
| FC2 | Author + blog directories | ☑ |
| FC3 | Author profile page | ☑ |
| FC4 | Home load more | ☑ |
| FCdev | Enough featured posts for grid | ☑ |

**Review approval:** approved 2026-07-07 — production baseline
