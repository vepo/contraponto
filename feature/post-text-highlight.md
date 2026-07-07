# Post text highlights, author curation & post responses

**Feature version:** 1  
**Status:** done  
**Requested:** 2026-05-19  
**Legacy PRD:** [docs/prd/post-text-highlight.md](../docs/prd/post-text-highlight.md)

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

## Architecture

### ADRs aplicáveis

| ADR | Relevância |
|-----|------------|
| [0002](../docs/adr/0002-backend-java-quarkus-jakarta-ee.md) | Backend |
| [0003](../docs/adr/0003-frontend-qute-htmx.md) | HTMX fragments |

### Design específico

| Area | Design |
|------|--------|
| Packages | `highlight`, `postresponse` |
| Events | Notifications on proposals via `NotificationType` |
| Anchors | Character offsets on live publication plain text |

## Changelog

### Initial implementation — 2026-06-01

**Version:** 1  
**Status:** done

#### Feature checklist

| ID | Criterion | Done |
|----|-----------|------|
| FC1 | Reader highlight save/remove on post | ☑ |
| FC2 | Author official highlight approval | ☑ |
| FC3 | Public note approval flow | ☑ |
| FC4 | Post response with backlink approval | ☑ |
| FCdev | Seed highlights on dev posts | ☑ |

**Development approval:** approved 2026-06-01 — tasks: T1–T12 (historical)

**Implementation notes:** See PRD for full route map; `highlight.js` on post pages; library in Reading hub.
