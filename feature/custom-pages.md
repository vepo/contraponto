# Custom pages

**Feature version:** 1  
**Status:** done  
**Production:** live

## Changelog

### Production baseline — 2026-07-07

**Version:** 1  
**Status:** done

**Production:** live — deployed capability

## Summary

**Custom pages** are Markdown content at scoped URLs: **global** (`/page/{slug}`), **main blog** (`/{username}/page/{slug}`), or **secondary blog** (`/{username}/{blogSlug}/page/{slug}`). **Footer/sidebar** placement via `PagePlacement`. **Editors** manage global pages; **blog owners** manage blog-scoped pages.

## Wireframe

| Screen | Route | Notes |
|--------|-------|-------|
| Public page | `/page/{slug}` or scoped | Server-rendered; cache-backed |
| Manage list | `GET /manage/pages` | Hub panel + pagination |
| New / Edit | `GET /pages/new`, `/pages/{id}/edit` | Title, slug, section, placement |
| Footer links | Global footer | Migration seed: sobre, contato, privacidade, termos |

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `custompage` |
| Schema | `tb_custom_pages`, `tb_custom_page_image_dependencies` |
| CDI | `CustomPageChangedEvent` → cache + sitemap invalidation |
| Filter | `CustomPageFilter` rewrites public GET to `/_custom_page/...` |
| Tests | `CustomPageTest`, `CustomPageManageTest`, `CustomPageCacheTest` |



### Risks

| Risk | Mitigation |
|------|------------|
| In-memory cache per node | Documented; refresh on change event |
| Reserved slugs | `CustomPagePaths.RESERVED_SEGMENTS` |
| Unpublished direct URL | 404 |

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Who edits global pages? | answered | **EDITOR**+ (`canManageApplicationPages`) |
| FQ2 | Hard delete vs unpublish? | answered | **Delete** removes row; `published` flag for draft |

## Architecture

### ADRs aplicáveis

| ADR | Relevância |
|-----|------------|
| [0003](../docs/adr/0003-frontend-qute-htmx.md) | Manage hub mutations |
| [0013](../docs/adr/0013-cdi-events-cross-context.md) | `CustomPageChangedEvent` |

### Design específico da feature

| Area | Design |
|------|--------|
| URLs | `CustomPagePaths.publicUrl(page)` — always `/page/` segment |
| Cache | `CustomPageCache` — published pages only |
| Access | `CustomPageAccess` — owner vs editor global |

### HTMX component model

| Component | Pattern |
|-----------|---------|
| Manage panel | Hub shell; save/delete → Toast + OOB hub |
| Public read | No HTMX — full page SSR |

#### Feature checklist

| ID | Criterion | Done |
|----|-----------|------|
| FC1 | Global footer pages (migration seed) | ☑ |
| FC2 | Blog-scoped custom pages | ☑ |
| FC3 | Manage CRUD | ☑ |
| FCdev | Footer pages preserved on dev reset | ☑ |

**Development approval:** approved — production baseline (shipped)
**Review approval:** approved 2026-07-07 — production baseline
