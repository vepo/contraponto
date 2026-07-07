# Feature change requests

One markdown file per **high-level capability**: `feature/<feature-slug>.md` (kebab-case).

**Mandatory process:** [development-process.mdc](../.cursor/rules/development-process.mdc) — PO → domain → architect (+ ADRs, HTMX model) → task modeller → **explicit task approval** → development squad (Java → HTMX → JS) → **review trio** → **review approval** → `done`. Agents: [.cursor/agents/](../.cursor/agents/). **ADR `Accepted`, task approval, and review approval require explicit human messages** — agents must not infer acceptance.

**ADRs:** transversal decisions in [docs/adr/](../docs/adr/). Baseline Accepted: [0001](../docs/adr/0001-record-architecture-decisions.md)–[0005](../docs/adr/0005-postgresql-database.md). Template: [docs/adr/template.md](../docs/adr/template.md).

**Legacy PRDs:** existing product specs remain in [docs/prd/](../docs/prd/) until migrated to `feature/<slug>.md`.

## Resolving `<feature-slug>`

See [change-request-analysis.mdc](../.cursor/rules/change-request-analysis.mdc) § Resolve `<feature-slug>`.

| Request type | Example slug |
|--------------|--------------|
| Personal reading list | `reading-list` (see [docs/prd/reading-list.md](../docs/prd/reading-list.md)) |
| Reader highlights | `post-text-highlight` (see [docs/prd/post-text-highlight.md](../docs/prd/post-text-highlight.md)) |
| Blog follow / subscribe | `blog-audience` |
| Git ↔ Jekyll sync | `git-sync` |
| ActivityPub / Fediverse | `activitypub-integration` |
| User-to-user messaging | `user-messaging` |
| In-app notification retention | `notification-retention` |

## Template

Copy into `feature/<feature-slug>.md`. Phases 2–7 add Architecture (+ HTMX model), Tasks, approval, squad implementation, review, done.

```markdown
# <Human-readable feature name>

**Feature version:** 1  
**Status:** planned | architecture-ready | tasks-ready | approved | in-progress | review-ready | done  
**Requested:** YYYY-MM-DD

## Summary

One paragraph: what is being asked and why.

## Wireframe

**Guide:** layout reference for UI — update when Scope or **FQ*n*** change.

| Field | Value |
|-------|-------|
| **Source** | ASCII below · N/A |
| **Last updated** | YYYY-MM-DD |

### Screen: `/example-route`

| Region | Elements | Notes |
|--------|----------|-------|
| Main | … | [ui-elements.md](../docs/ui-elements.md) |

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | e.g. `post`, `readinglist` |
| Packages / files | Main touch points |
| Routes / templates | `@Path`, Qute, HTMX |
| UI | Templates, CSS bundle |
| Schema / seed | `db/migration/V*.sql`, **`dev-import.sql`** |
| Tests | `@WebTest`, unit, ArchUnit |
| Docs | domain-spec, feature-catalog, ADRs, ARCHITECTURE |

### Risks

…

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | … | open | |

## Architecture

See [architecture-design.mdc](../.cursor/rules/architecture-design.mdc).

### ADRs aplicáveis

| ADR | Relevância |
|-----|------------|
| [0002](../docs/adr/0002-backend-java-quarkus-jakarta-ee.md) | Backend |
| [0003](../docs/adr/0003-frontend-qute-htmx.md) | Qute + HTMX |

### Design específico da feature

| Area | Design |
|------|--------|
| Bounded contexts | … |
| Packages / layers | Endpoint → Service → Repository |
| Routes / templates | `*Paths`, Qute, HTMX targets |
| Schema / seed | Flyway file, `dev-import.sql` rows |
| CDI events | Producers / observers |
| Tests | `*WebTest` scenarios |

### HTMX component model

| Component id | Route | Activator | Target/swap | Events out | Events in | JS | Auth allowlist |
|--------------|-------|-----------|-------------|------------|-----------|-----|----------------|
| `#example` | GET /components/… | `load` | self innerHTML | — | — | none | No |

### HTMX interaction diagram

```
[Activator] → GET /components/… → swap #target
```

### Architecture questions (AQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | … | open | |

## Changelog

### <Change name> — YYYY-MM-DD

**Version:** 1  
**Status:** planned | architecture-ready | tasks-ready | approved | in-progress | review-ready | done

**Description:** …

**Domain model:** N/A | updated YYYY-MM-DD (see domain-spec)

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| — | None identified |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | … | FQ1 | ☐ |
| FCdev | `dev-import.sql` covers this feature's happy path | dev-import-sql-safety | ☐ |

#### Tasks (phase 3) — layer tags

| ID | Layer | Task | Depends | Expected outcome | Tests | Done |
|----|-------|------|---------|------------------|-------|------|
| T1-java | java | … | — | … | TC1 | ☐ |
| T1-htmx | htmx | … | T1-java | … | TC1 | ☐ |
| T1-js | js | … | T1-htmx | … | — | ☐ |
| Tdev | doc | Update `dev-import.sql` + feature-catalog § Dev personas | … | … | FCdev | ☐ |

#### Test coverage (phase 3)

| ID | Kind | Test | Covers | Done |
|----|------|------|--------|------|
| TC1 | unit \| rest \| web \| htmx-contract \| arch | … | T1-java, T1-htmx | ☐ |

**Development approval:** pending | approved YYYY-MM-DD — tasks: T1-java, T1-htmx

#### Review findings (phase 6)

| Reviewer | Severity | Location | Finding | Status |
|----------|----------|----------|---------|--------|
| — | — | — | (pending review) | — |

**Review approval:** pending | approved YYYY-MM-DD

**Implementation notes:** (after done)
```

## Feature index

| Capability | File | Status |
|------------|------|--------|
| Personal reading list | [reading-list.md](reading-list.md) | done |
| Post text highlights & responses | [post-text-highlight.md](post-text-highlight.md) | done |
| ActivityPub Fediverse integration | [activitypub-integration.md](activitypub-integration.md) | done |
| Notification retention | [notification-retention.md](notification-retention.md) | done |
| User messaging | [user-messaging.md](user-messaging.md) | done |

Add a row here when creating `feature/<slug>.md`.

## Versioning

- **Feature version** — highest changelog Version in the file.
- **FQ*n*** / **AQ*n*** — product vs technical questions.
- **ADRs** — transversal decisions; cite by number in Architecture.
- **FC*n***, **T*n*** (`*-java`, `*-htmx`, `*-js`), **TC*n*** — per changelog entry; recheck FC before `done`.
- **Review findings** — phase 6; no `done` without **Review approval**.
