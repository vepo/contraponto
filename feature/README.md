# Feature change requests

One markdown file per **high-level capability**: `feature/<feature-slug>.md` (kebab-case).

**Mandatory process:** [development-process.mdc](../.cursor/rules/development-process.mdc) — PO → domain → architect (+ ADRs, HTMX model) → task modeller → **explicit task approval** → development squad (Java → HTMX → JS) → **review trio** → **review approval** → `done`. Agents: [.cursor/agents/](../.cursor/agents/). **ADR `Accepted`, task approval, and review approval require explicit human messages** — agents must not infer acceptance.

**ADRs:** transversal decisions in [docs/adr/](../docs/adr/). Baseline Accepted: [0001](../docs/adr/0001-record-architecture-decisions.md)–[0005](../docs/adr/0005-postgresql-database.md). Template: [docs/adr/template.md](../docs/adr/template.md).

**Legacy PRDs:** existing product specs remain in [docs/prd/](../docs/prd/) until migrated to `feature/<slug>.md`.

## Resolving `<feature-slug>`

See [change-request-analysis.mdc](../.cursor/rules/change-request-analysis.mdc) § Resolve `<feature-slug>`.

| Request type | Example slug |
|--------------|--------------|
| Authentication | `authentication` |
| Multi-blog | `multi-blog` |
| Post publishing | `post-publishing` |
| Comments | `post-comments` |
| Blog follow / subscribe | `blog-audience` |
| Personal reading list | `reading-list` (see [docs/prd/reading-list.md](../docs/prd/reading-list.md)) |
| Reader highlights | `post-text-highlight` (see [docs/prd/post-text-highlight.md](../docs/prd/post-text-highlight.md)) |
| Git ↔ Jekyll sync | `git-sync` |
| Custom pages | `custom-pages` |
| Home & discovery | `home-discovery` |
| Search | `search` |
| Tags & series | `tags-and-series` |
| RSS | `rss-syndication` |
| SEO | `seo` |
| Editor review | `editor-review` |
| User admin | `user-administration` |
| Image library | `image-library` |
| Author profile | `author-profile` |
| Dashboard analytics | `dashboard-analytics` |
| ActivityPub / Fediverse | `activitypub-integration` |
| Bluesky platform syndication | `bluesky-platform-syndication` |
| Newsletter with subscription-gated content | `newsletter-gated-content` |
| Platform support page (PIX) | `platform-support-pix` |
| User-to-user messaging | `user-messaging` |
| In-app notification retention | `notification-retention` |

## Template

Copy into `feature/<feature-slug>.md`. Phases 2–7 add Architecture (+ HTMX model), Tasks, approval, squad implementation, review, done.

```markdown
# <Human-readable feature name>

**Feature version:** 1  
**Status:** done  
**Production:** live

## Changelog

### Production baseline — YYYY-MM-DD

**Version:** 1  
**Status:** done

**Production:** live — deployed capability

**Description:** …

**Domain model:** N/A | updated YYYY-MM-DD (see domain-spec)

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| — | None identified |

## Summary

…

## Wireframe

…

## Impact

…

### Risks

…

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | … | open | |

## Architecture

### ADRs aplicáveis

…

### Design específico da feature

…

### HTMX component model

…

### Architecture questions (AQ*n*)

…

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | … | FQ1 | ☑ |
| FCdev | `dev-import.sql` covers this feature's happy path | dev-import-sql-safety | ☑ |

#### Tasks (phase 3) — layer tags

| ID | Layer | Task | Depends | Expected outcome | Tests | Done |
|----|-------|------|---------|------------------|-------|------|
| T1-java | java | … | — | … | TC1 | ☑ |

#### Test coverage (phase 3)

| ID | Kind | Test | Covers | Done |
|----|------|------|--------|------|
| TC1 | unit \| rest \| web | … | T1-java | ☑ |

**Development approval:** approved — production baseline (shipped)

**Review approval:** approved YYYY-MM-DD — production baseline

**Implementation notes:** …
```

Shipped capabilities use a **single Production baseline** changelog entry containing Summary, Wireframe, Impact, Architecture, checklist, and approvals. **Future changes** add new `### <Change name> — date` entries below the baseline (increment **Feature version** in the file header).

## Feature index

| Capability | File | Status |
|------------|------|--------|
| Authentication & account access | [authentication.md](authentication.md) | done |
| Multi-blog per author | [multi-blog.md](multi-blog.md) | done |
| Post publishing & version history | [post-publishing.md](post-publishing.md) | done (v1); architecture-ready (v2 — scheduled publishing) |
| Post comments & moderation | [post-comments.md](post-comments.md) | done |
| Blog audience (follow, subscribe, delivery) | [blog-audience.md](blog-audience.md) | done |
| Personal reading list | [reading-list.md](reading-list.md) | done |
| Post text highlights & responses | [post-text-highlight.md](post-text-highlight.md) | done |
| Git ↔ Jekyll sync | [git-sync.md](git-sync.md) | done |
| Custom pages | [custom-pages.md](custom-pages.md) | done |
| Home & discovery | [home-discovery.md](home-discovery.md) | done |
| Search | [search.md](search.md) | done |
| Tags & series | [tags-and-series.md](tags-and-series.md) | done |
| RSS syndication | [rss-syndication.md](rss-syndication.md) | done |
| SEO & crawlability | [seo.md](seo.md) | done |
| Editor review & featured curation | [editor-review.md](editor-review.md) | done |
| User administration | [user-administration.md](user-administration.md) | done |
| Image library | [image-library.md](image-library.md) | done |
| Author profile & appearance | [author-profile.md](author-profile.md) | done |
| Dashboard & platform analytics | [dashboard-analytics.md](dashboard-analytics.md) | done |
| ActivityPub Fediverse integration | [activitypub-integration.md](activitypub-integration.md) | done |
| Bluesky platform syndication | [bluesky-platform-syndication.md](bluesky-platform-syndication.md) | planned |
| Newsletter with subscription-gated content | [newsletter-gated-content.md](newsletter-gated-content.md) | planned |
| Platform support page (PIX) | [platform-support-pix.md](platform-support-pix.md) | planned |
| Notification retention | [notification-retention.md](notification-retention.md) | done |
| User messaging | [user-messaging.md](user-messaging.md) | done |

Add a row here when creating `feature/<slug>.md`.

### Production baseline (2026-07-07)

All **22** shipped capabilities are documented as **`### Production baseline`** changelog entries with **`Production: live`**. The application is in production; new work adds subsequent changelog entries under the same file. Open **FQ*n*** / **AQ*n*** rows in each file mark known follow-ups, not missing baseline scope.

## Versioning

- **Feature version** — highest changelog Version in the file.
- **FQ*n*** / **AQ*n*** — product vs technical questions.
- **ADRs** — transversal decisions; cite by number in Architecture.
- **FC*n***, **T*n*** (`*-java`, `*-htmx`, `*-js`), **TC*n*** — per changelog entry; recheck FC before `done`.
- **Review findings** — phase 6; no `done` without **Review approval**.
