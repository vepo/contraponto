# Personal reading list

**Feature version:** 1  
**Status:** done  
**Requested:** 2026-05-19  
**Legacy PRD:** [docs/prd/reading-list.md](../docs/prd/reading-list.md)

## Summary

Per-user **reading list**: save published posts for later, **mark as read**, triage unread items in the **Reading hub** (`/reading/saved`). Private to the saving reader; does not affect author publishing.

## Wireframe

| Screen | Route | Notes |
|--------|-------|-------|
| Reading hub — Saved | `GET /reading/saved` | Tabs: Unread / All; library-style rows |
| Post — Save control | Post action bar | **Salvar** / unread badge / **Lido** |

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `readinglist` (reader engagement) |
| Routes | `/reading/saved`, `/forms/posts/{id}/reading-list`, `/forms/reading-list/{id}/read` |
| Schema | `tb_reading_list_items` |
| Tests | `ReadingList*Test`, `ReadingList*WebTest` |
| Docs | domain-spec, feature-catalog § Reading hub |

## Architecture

### ADRs aplicáveis

| ADR | Relevância |
|-----|------------|
| [0002](../docs/adr/0002-backend-java-quarkus-jakarta-ee.md) | Backend |
| [0003](../docs/adr/0003-frontend-qute-htmx.md) | HTMX post action bar |

### Design específico

| Area | Design |
|------|--------|
| Package | `dev.vepo.contraponto.readinglist` |
| Entity | `ReadingListItem` — post + user + state |
| Service | `ReadingListService.buildActionView`, save/read/remove |
| UI | Server-rendered on post page; lazy HTMX only on auth refresh |

## Changelog

### Initial implementation — 2026-06-23

**Version:** 1  
**Status:** done

**Description:** Reading list MVP shipped per PRD.

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Save / remove / mark read on post page | PRD §3 | ☑ |
| FC2 | Unread + All tabs in Reading hub | PRD §5 | ☑ |
| FC3 | Guest sees sign-in to save | PRD §7 | ☑ |
| FCdev | `dave` has sample items in dev-import | dev-import | ☑ |

**Development approval:** approved 2026-06-23 — tasks: T1–T8 (historical)

**Implementation notes:** Package `readinglist`; hub section slug `saved`; forms under `/forms/posts/{postId}/reading-list`. Post page SSR reading-list action (no `load` trigger) for CLS.
