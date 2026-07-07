# Personal reading list

**Feature version:** 1  
**Status:** done  
**Production:** live

## Changelog

### Production baseline — 2026-06-23

**Version:** 1  
**Status:** done

**Production:** live — deployed capability

**Description:** Reading list MVP shipped per PRD.

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



### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Private to saving reader? | answered | **Yes** — no author visibility |
| FQ2 | Remove from list vs mark read? | answered | Separate actions; unread badge on post |

### Risks

| Risk | Mitigation |
|------|------------|
| CLS on post page | SSR reading-list action bar (no `load` trigger) |

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
| Schema | `tb_reading_list_items` |
| Tests | `ReadingList*Test`, `ReadingList*WebTest` |

### HTMX component model

| Component id | Route | Activator | Target/swap | Events in | JS |
|--------------|-------|-----------|-------------|-----------|-----|
| Post action bar | SSR on post | — | — | `{authRefreshTrigger}` on auth | none |
| Save / mark read | `POST /forms/posts/{id}/reading-list`, `POST /forms/reading-list/{id}/read` | Button click | action bar swap | — | none |
| Reading hub saved | `GET /reading/saved` | Hub nav | full page / hub panel | — | none |

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Private to saving reader? | answered | **Yes** — no author visibility |
| FQ2 | Remove from list vs mark read? | answered | Separate actions; unread badge on post |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Save / remove / mark read on post page | PRD §3 | ☑ |
| FC2 | Unread + All tabs in Reading hub | PRD §5 | ☑ |
| FC3 | Guest sees sign-in to save | PRD §7 | ☑ |
| FCdev | `dave` has sample items in dev-import | dev-import | ☑ |

**Development approval:** approved 2026-06-23 — tasks: T1–T8 (historical)

**Review approval:** approved 2026-07-07 — production baseline

**Implementation notes:** Package `readinglist`; hub section slug `saved`; forms under `/forms/posts/{postId}/reading-list`. Post page SSR reading-list action (no `load` trigger) for CLS.
