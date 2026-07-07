# Multi-blog per author

**Feature version:** 1  
**Status:** done  
**Production:** live

## Changelog

### Production baseline — 2026-07-07

**Version:** 1  
**Status:** done

**Production:** live — deployed capability

**Description:** Multi-blog, management UI, and subdomain URLs.

## Summary

Each **user** owns one **main blog** (auto-created at registration, `main = true`) and may create **secondary blogs** with unique slugs per owner. Public **blog home** lists published posts; **blog subdomain** URLs shorten paths when enabled ([ADR-0011](../docs/adr/0011-blog-subdomain-urls.md)). Owners manage blogs from Writing hub; **editors** list all blogs from Manage hub.

## Wireframe

| Screen | Route | Notes |
|--------|-------|-------|
| Main blog home | `GET /{username}` | Hero, grid, RSS, audience widget |
| Secondary blog | `GET /{username}/{blogSlug}` | Same layout |
| Writing — Blogs | `GET /writing/blogs` | New / Edit / Settings / Deactivate |
| Manage — Blogs | `GET /manage/blogs` | Editor platform-wide list |
| Blog edit | `GET /blogs/{id}/edit` | Core fields + Git section link |
| Blog settings | `GET /blogs/{id}/settings` | Extended settings (`full=true`) |

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `blog` |
| Packages | `blog.*`, `BlogSubdomainFilter`, `BlogPublicUrlService` |
| Schema | `tb_blogs` — `owner_id`, `slug`, `main`, `active`, `banner_id`, git fields |
| Tests | `BlogTest`, `SecondaryBlogHomeTest`, `BlogAccessTest`, `BlogManageTest`, `BlogSubdomainIntegrationTest` |
| Docs | [blog-subdomain-urls.md](../docs/blog-subdomain-urls.md), feature-catalog |



### Risks

| Risk | Mitigation |
|------|------------|
| Main blog cannot be deactivated | `BlogAccess.canDeactivate` blocks `main` |
| Subdomain off in dev | `app.blog-subdomain.enabled=false` default |
| Editor can deactivate others' blogs | Owner-only edit; editor deactivate only |

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Hard delete blogs? | answered | **No** — soft deactivate (`active=false`) |
| FQ2 | Rename main blog slug independently of username? | answered | **No** — slug tied at creation |
| FQ3 | Multi-blog profile at `GET /{username}`? | open | Catalog says "lists blogs"; code shows main blog home — profile is `/authors/{username}` |

## Architecture

### ADRs aplicáveis

| ADR | Relevância |
|-----|------------|
| [0002](../docs/adr/0002-backend-java-quarkus-jakarta-ee.md) | Backend |
| [0003](../docs/adr/0003-frontend-qute-htmx.md) | Hub navigation |
| [0011](../docs/adr/0011-blog-subdomain-urls.md) | Dual URL model |

### Design específico da feature

| Area | Design |
|------|--------|
| Authorization | `BlogAccess` — owner edit; editor `canListAll` |
| URLs | `BlogPaths.extractUrl(blog)`; templates via `TemplateExtensions.url` |
| No BlogService | Orchestration in `BlogSaveEndpoint`, `BlogManageEndpoint`, `BlogEndpoint` |
| Banner | `BlogBannerService` — effective banner = blog or user default |
| Description | Markdown on public home via `BlogDescriptionRenderer` |

### HTMX component model

| Component id | Route | Activator | Target/swap | Events out | Events in | JS |
|--------------|-------|-----------|-------------|------------|-----------|-----|
| Hub panel | `GET /writing/blogs`, `/manage/blogs` | Hub nav | `main` outerHTML | — | — | `main.js` |
| Blog grid | `GET /{username}/components/grid` | Load more | grid region | — | — | none |
| `#blog-audience-{id}` | `GET /components/blogs/{id}/audience` | load / auth | self outerHTML | — | `{authRefreshTrigger}` | none |
| Blog save | `POST /forms/blogs` | Submit | Toast → hub shell | toast | — | `toast.js` |
| Deactivate | `DELETE /forms/blogs/{id}` | Row action | hub via toast | toast | — | `toast.js` |

### Architecture questions (AQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | Subdomain canonical URLs? | answered | `BlogPublicUrlService` when enabled |

#### Feature checklist

| ID | Criterion | Done |
|----|-----------|------|
| FC1 | Main + secondary public homes | ☑ |
| FC2 | Author blog CRUD in Writing hub | ☑ |
| FC3 | Editor platform blog list | ☑ |
| FC4 | Load more on blog grids | ☑ |
| FCdev | `bob` + `architecture-notes`, `vepo` + `notas` | ☑ |

**Development approval:** approved — production baseline (shipped)
**Review approval:** approved 2026-07-07 — production baseline

**Implementation notes:** Secondary slug cannot equal username. Git settings on blog edit — see [git-sync.md](git-sync.md).
