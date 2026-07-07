# Image library

**Feature version:** 1  
**Status:** done  
**Production:** live

## Changelog

### Production baseline — 2026-07-07

**Version:** 1  
**Status:** done

**Production:** live — deployed capability

## Summary

Per-author **image library** in Writing hub (`/writing/images`): upload, search by alt/path, edit alt text, delete unused images. **Picker modal** for write editor and covers. Binary serve via `/api/images/{uuid}`.

## Wireframe

| Screen | Route | Notes |
|--------|-------|-------|
| Images hub | `GET /writing/images?q=` | Search + pagination |
| Picker modal | `GET /components/images/picker` | Write/cover selection |
| Legacy redirect | `GET /blogs/{blogId}/images` | → `/writing/images` |

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `image` |
| Schema | `tb_images`, dependency tables on post/custom page |
| Tests | `ImageTest`, `ImageControlTest`, `ImagePickerTest`, `ImageServiceTest` |



### Risks

| Risk | Mitigation |
|------|------------|
| TRUNCATE tb_images in dev-import | Must null FKs first — dev-import-sql-safety |

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Delete image in use? | answered | Blocked when dependencies exist |

## Architecture

### HTMX component model

| Component | Route | Activator |
|-----------|-------|-----------|
| Picker grid | `GET /components/images/picker/grid` | Modal open / search |
| Alt save | `POST /forms/images/{uuid}/alt` | Form submit |

#### Feature checklist

| ID | Criterion | Done |
|----|-----------|------|
| FC1 | Writing hub images section | ☑ |
| FC2 | Picker in write editor | ☑ |
| FC3 | Upload + serve API | ☑ |
| FCdev | Images on seed blogs/posts | ☑ |

**Review approval:** approved 2026-07-07 — production baseline
