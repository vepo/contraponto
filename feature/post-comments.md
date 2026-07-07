# Post comments & moderation

**Feature version:** 1  
**Status:** done  
**Production:** live

## Changelog

### Production baseline — 2026-07-07

**Version:** 1  
**Status:** done

**Production:** live — deployed capability

## Summary

Signed-in **readers** comment on **published** posts. Non-owner comments start **Pending** until the **post owner** approves or rejects. **Post owner** comments are **auto-approved**. **Replies** nest under approved parents. Moderation on the post page or **Manage → Comments** inbox.

## Wireframe

| Screen | Route | Notes |
|--------|-------|-------|
| Post comments section | lazy `GET …/components/comments` | Form + pending queue + roots |
| Reply thread | `GET …/components/comments/{id}/replies` | Lazy load |
| Manage inbox | `GET /manage/comments` | Pending across author's posts |

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `comment` |
| Schema | `tb_post_comments` — status, parent_id, root_id |
| Notifications | `NEW_COMMENT` when pending (not auto-approved) |
| Tests | `PostCommentTest`, `CommentManageTest`, `CommentAfterLoginWebTest` |



### Risks

| Risk | Mitigation |
|------|------------|
| Spam comments | Owner moderation required |
| Guest comments | Login modal gate |

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Max comment length? | answered | **2000** chars |
| FQ2 | Editor moderates any post? | answered | **No** — post owner only |

## Architecture

### ADRs aplicáveis

| ADR | Relevância |
|-----|------------|
| [0003](../docs/adr/0003-frontend-qute-htmx.md) | Lazy comments fragment |

### Design específico da feature

| Area | Design |
|------|--------|
| Service | `PostCommentService` — create, reply, approve, reject, visibility |
| Repository | `PostCommentRepository` — roots, replies, pending pages |
| Gate | Published posts only (`loadPublishedPost`) |

### HTMX component model

| Component id | Route | Activator | Target/swap | Events out | Events in | JS |
|--------------|-------|-----------|-------------|------------|-----------|-----|
| `#comments` | `GET …/components/comments` | `load`, `{authRefreshTrigger}` | self innerHTML | — | auth refresh | none |
| Comment form | `POST /forms/posts/{id}/comments` | Submit | `#comments` | toast optional | — | none |
| Reply form | `POST …/comments/{parentId}/replies` | Submit | `#comments` | — | — | none |
| Approve/reject (post) | `POST …/approve|reject` | Button | `#comments` | — | — | none |
| Approve/reject (manage) | `POST …?from=manage` | Button | `main` hub shell | toast | — | none |
| View replies | `GET …/{id}/replies` | Link | `#replies-{id}` | — | — | none |

### HTMX interaction diagram

```
Post page load → GET …/components/comments → #comments innerHTML
Submit comment → POST …/comments → refresh #comments
Manage approve → POST …?from=manage → swap main (hub shell)
```

#### Feature checklist

| ID | Criterion | Done |
|----|-----------|------|
| FC1 | Root comment + guest gate | ☑ |
| FC2 | Reply threads | ☑ |
| FC3 | Owner approve/reject on post | ☑ |
| FC4 | Manage comments inbox | ☑ |
| FCdev | Sample pending/approved comments on seed posts | ☑ |

**Development approval:** approved — production baseline (shipped)
**Review approval:** approved 2026-07-07 — production baseline
