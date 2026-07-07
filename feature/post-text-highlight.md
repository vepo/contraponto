# Post text highlights, author curation & post responses

**Feature version:** 1  
**Status:** done  
**Production:** live

## Changelog

### Production baseline — 2026-06-01

**Version:** 1  
**Status:** done

**Production:** live — deployed capability

## Summary

Readers save **post text highlights** (private by default). **Common highlight proposals** and **public highlight notes** require **author approval**. **Post responses** link reader essays back to the original after approval.

## Wireframe

| Screen | Route | Notes |
|--------|-------|-------|
| Post body | Published post | Selection → highlight; action bar; official highlights block |
| Highlights library | `/reading/highlights` | Reader's saved passages |
| Notes library | `/reading/notes` | Reader's notes |
| Author moderation | Post page / notifications | Approve/reject proposals, notes, responses |

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `highlight`, `postresponse` |
| Schema | `tb_post_text_highlights`, `tb_official_highlights`, `tb_highlight_notes`, `tb_post_responses`, … |
| Tests | `Highlight*Test`, `Highlight*WebTest`, `PostResponse*Test` |



### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Reader highlights private by default? | answered | **Yes** |
| FQ2 | Author must approve public notes? | answered | **Yes** |
| FQ3 | Post responses require approval? | answered | **Yes** — backlink after approval |

### Risks

| Risk | Mitigation |
|------|------------|
| Offset drift on republish | Anchors on live publication plain text; may invalidate on major edits |

## Architecture

### ADRs aplicáveis

| ADR | Relevância |
|-----|------------|
| [0002](../docs/adr/0002-backend-java-quarkus-jakarta-ee.md) | Backend |
| [0003](../docs/adr/0003-frontend-qute-htmx.md) | HTMX fragments |
| [0013](../docs/adr/0013-cdi-events-cross-context.md) | Notification on proposals |

### Design específico

| Area | Design |
|------|--------|
| Packages | `highlight`, `postresponse` |
| Events | Notifications on proposals via `NotificationType` |
| Anchors | Character offsets on live publication plain text |
| JS companion | `js/highlight.js` on post pages — text selection UI |

### HTMX component model

| Component id | Route | Activator | Target/swap | JS |
|--------------|-------|-----------|-------------|-----|
| Highlight actions | `POST /forms/posts/{postId}/highlights` | Selection toolbar | highlight region | `highlight.js` |
| Note modal | `GET /forms/highlights/{id}/notes/modal` | Add note | `#modal-container` | none |
| Reading hub | `GET /reading/highlights`, `/reading/notes` | Hub nav | hub panel | none |
| Official highlights block | SSR on post | load | — | `highlight.js` |

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Reader highlights private by default? | answered | **Yes** |
| FQ2 | Author must approve public notes? | answered | **Yes** |
| FQ3 | Post responses require approval? | answered | **Yes** — backlink after approval |

#### Feature checklist

| ID | Criterion | Done |
|----|-----------|------|
| FC1 | Reader highlight save/remove on post | ☑ |
| FC2 | Author official highlight approval | ☑ |
| FC3 | Public note approval flow | ☑ |
| FC4 | Post response with backlink approval | ☑ |
| FCdev | Seed highlights on dev posts | ☑ |

**Development approval:** approved 2026-06-01 — tasks: T1–T12 (historical)

**Review approval:** approved 2026-07-07 — production baseline

**Implementation notes:** See PRD for full route map; `highlight.js` on post pages; library in Reading hub.
