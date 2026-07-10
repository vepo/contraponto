# Agent instructions (Contraponto)

Read these before changing code or tests:

| Document | Purpose |
|----------|---------|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Stack, patterns, URL map, schema, feature workflows |
| [docs/adr/](docs/adr/) | **Architecture Decision Records** — transversal decisions (GiG Cymru template) |
| [feature/](feature/) | Feature analysis, tasks, approval — start at [feature/README.md](feature/README.md) |
| [docs/domain-specification.md](docs/domain-specification.md) | Ubiquitous language, bounded contexts, invariants |
| [docs/htmx-events.md](docs/htmx-events.md) | HTMX lifecycle, custom events, scoped refresh, auth allowlist |
| [docs/conventions-checklist.md](docs/conventions-checklist.md) | Gaps between docs and code; agent setup status |
| [docs/technical-decisions-backlog.md](docs/technical-decisions-backlog.md) | Open ADRs / FQ/AQ before coding |
| [docs/cdi-events.md](docs/cdi-events.md) | CDI event producers, observers, transactional boundaries |
| [docs/application-guidelines.md](docs/application-guidelines.md) | Route-level UX flows (update when routes change) |
| [docs/rest-url-guide.md](docs/rest-url-guide.md) | REST URL segment taxonomy and full path catalog |
| [docs/feature-catalog.md](docs/feature-catalog.md) | UI feature index with step counts and navigation paths |
| [docs/ui-guidelines.md](docs/ui-guidelines.md) | Visual design system (UX, page flows) |
| [docs/ui-palette.md](docs/ui-palette.md) | Design tokens — check before UI/CSS changes |
| [docs/ui-elements.md](docs/ui-elements.md) | CSS class catalog and main/manage/write bundles |
| [docs/git-jekyll-convention.md](docs/git-jekyll-convention.md) | Git ↔ Jekyll sync layout |
| [docs/deployment.md](docs/deployment.md) | Production checklist |
| [docs/blog-subdomain-urls.md](docs/blog-subdomain-urls.md) | Platform vs author subdomain URLs |
| [docs/mastodon-remote-account-resolution.md](docs/mastodon-remote-account-resolution.md) | Mastodon remote profile discovery flow + Contraponto gap analysis |
| [docs/greenfield-deployment-tutorial.md](docs/greenfield-deployment-tutorial.md) | First deploy on a new server |
| [src/test/resources/docker-smoke/README.md](src/test/resources/docker-smoke/README.md) | Prod-faithful smoke stack |
| [.github/workflows/README.md](.github/workflows/README.md) | CI pipeline and test publishing |
| [docs/prd/](docs/prd/) | Legacy PRDs (migrate to `feature/` over time) |
| [.cursor/rules/](.cursor/rules/) | Cursor rules — see pillars below |
| [.cursor/agents/](.cursor/agents/) | Project subagents (TDD, domain, docs) |

**Baseline ADRs (Accepted):** [ADR-0002](docs/adr/0002-backend-java-quarkus-jakarta-ee.md) Java + Quarkus; [ADR-0003](docs/adr/0003-frontend-qute-htmx.md) Qute + HTMX; [ADR-0004](docs/adr/0004-package-naming-dev-vepo.md) `dev.vepo.contraponto`; [ADR-0005](docs/adr/0005-postgresql-database.md) PostgreSQL + Flyway; [ADR-0006](docs/adr/0006-activitypub-federation.md)–[0008](docs/adr/0008-activitypub-actor-identity.md) ActivityPub federation; [0009](docs/adr/0009-user-messaging-retention.md)–[0010](docs/adr/0010-notification-retention.md) retention policies; [0015](docs/adr/0015-federation-outbound-fetch-ssrf.md)–[0016](docs/adr/0016-account-deletion-audit-retention.md). **Proposed (awaiting acceptance):** [0011](docs/adr/0011-blog-subdomain-urls.md)–[0014](docs/adr/0014-session-store.md); [0017](docs/adr/0017-per-blog-git-credentials-ssh.md) Git credentials + SSH.

**Feature docs:** [feature/README.md](feature/README.md) — 22 shipped capabilities documented (retroactive 2026-07-07).

**Development process:** [development-process.mdc](.cursor/rules/development-process.mdc) — seven phases with role agents (PO → domain → architect → modeller → squad → review → done). **Manual acceptance only:** ADRs → explicit user message; tasks → explicit task IDs (phase 4); review → explicit review approval (phase 7). Agents must not auto-accept ADRs, approve tasks, or mark `done`.

## Agents vs commands

| Surface | Location | Purpose |
|---------|----------|---------|
| **Role agents** | `.cursor/agents/*.md` | PO, architect, modeller, squad (Java/HTMX/JS), reviewers, TDD, domain, docs |
| **Commands** | `.cursor/commands/*.md` | fix tests, Sonar loop, coverage, structure review |

## Rules — four pillars (always on)

| Pillar | Rule | Covers |
|--------|------|--------|
| 1. Building the model | [domain-model.mdc](.cursor/rules/domain-model.mdc) | Domain spec, ubiquitous language, pre-change gate |
| 2. Testing | [contraponto-testing.mdc](.cursor/rules/contraponto-testing.mdc) | Tiered `mvn` commands, impact map |
| 3. Coding quality | [static-analysis.mdc](.cursor/rules/static-analysis.mdc) | Finish gate, `GITHUB_ACTIONS=true ./mvnw verify` |
| 4. Platform usage | [contraponto-core.mdc](.cursor/rules/contraponto-core.mdc) | Quarkus, Qute, HTMX, PostgreSQL |

**Process (always on):** [development-process.mdc](.cursor/rules/development-process.mdc), [change-request-analysis.mdc](.cursor/rules/change-request-analysis.mdc), [architecture-design.mdc](.cursor/rules/architecture-design.mdc), [adr.mdc](.cursor/rules/adr.mdc), [dev-import-sql-safety.mdc](.cursor/rules/dev-import-sql-safety.mdc), [development-experience.mdc](.cursor/rules/development-experience.mdc), [feature-catalog.mdc](.cursor/rules/feature-catalog.mdc), [readme.mdc](.cursor/rules/readme.mdc).

**Dev seed:** every schema/feature change must update [`dev-import.sql`](src/main/resources/dev-import.sql) — see [dev-import-sql-safety.mdc](.cursor/rules/dev-import-sql-safety.mdc).

## File-scoped rules (selected)

| Rule | Globs | Topic |
|------|-------|-------|
| [contraponto-java.mdc](.cursor/rules/contraponto-java.mdc) | `**/*.java` | Style, logging |
| [contraponto-format-imports.mdc](.cursor/rules/contraponto-format-imports.mdc) | `**/*.java` | Formatter + imports |
| [contraponto-tests.mdc](.cursor/rules/contraponto-tests.mdc) | `src/test/**` | `App`, `Given`, `@WebTest` |
| [contraponto-test-failure-diagnosis.mdc](.cursor/rules/contraponto-test-failure-diagnosis.mdc) | `src/test/**` | Failure reports |
| [contraponto-javascript.mdc](.cursor/rules/contraponto-javascript.mdc) | `js/*.js`, templates | HTMX-first |
| [contraponto-css.mdc](.cursor/rules/contraponto-css.mdc) | `style/*.css`, templates | Tokens, bundles |
| [contraponto-seo.mdc](.cursor/rules/contraponto-seo.mdc) | templates | Meta, crawlable links |
| [documentation.mdc](.cursor/rules/documentation.mdc) | `docs/**`, `feature/**` | Docs maintenance |
| [dev-import-sql-safety.mdc](.cursor/rules/dev-import-sql-safety.mdc) | `dev-import.sql`, migrations | Mandatory dev seed |

Full rule index: [.cursor/rules/](.cursor/rules/) (layered architecture, Tell Don't Ask, Law of Demeter, JPA, pagination, confirm modals, …).

## Project agents

### Analysis and planning (phases 1–3)

| Agent | Phase | When to delegate |
|-------|-------|------------------|
| [product-owner](.cursor/agents/product-owner.md) | 1 | Feature doc, wireframe, FQ*n*, cross-feature impact |
| [domain-model](.cursor/agents/domain-model.md) | 1b | domain-spec ubiquitous language |
| [architect](.cursor/agents/architect.md) | 2 | ADRs, Architecture, HTMX component model, htmx-events.md |
| [task-modeller](.cursor/agents/task-modeller.md) | 3 | Layer-tagged tasks T*n*-java/htmx/js, TC mapping |

### Development squad (phase 5)

| Agent | Layer | When to delegate |
|-------|-------|------------------|
| [java-developer](.cursor/agents/java-developer.md) | java | Endpoints, services, repos, Flyway, HtmxTriggers |
| [htmx-developer](.cursor/agents/htmx-developer.md) | htmx | Qute templates, hx-* wiring |
| [javascript-developer](.cursor/agents/javascript-developer.md) | js | `js/*.js` when Architect approved JS companion |
| [tdd-red](.cursor/agents/tdd-red.md) | — | Failing test for current layer task |
| [tdd-green](.cursor/agents/tdd-green.md) | — | Minimal code to pass Red |
| [tdd-refactor](.cursor/agents/tdd-refactor.md) | — | Refactor with tests green |

### Review (phase 6, readonly)

| Agent | When to delegate |
|-------|------------------|
| [java-reviewer](.cursor/agents/java-reviewer.md) | Layering, auth, backend HTMX contracts, tests |
| [htmx-reviewer](.cursor/agents/htmx-reviewer.md) | Templates vs HTMX model and htmx-events.md |
| [javascript-reviewer](.cursor/agents/javascript-reviewer.md) | JS necessity, module quality, swap guards |

### Finish (phase 7)

| Agent | When to delegate |
|-------|------------------|
| [docs-sync](.cursor/agents/docs-sync.md) | feature-catalog, README, htmx-events, ARCHITECTURE |

**Workflow:** PO → domain → architect → modeller → **you approve tasks** → squad (java → htmx → js per task) → review trio → **you approve review** → docs-sync → `done`.

## Commands

| Command | Purpose |
|---------|---------|
| [fix_tests.md](.cursor/commands/fix_tests.md) | Loop until tests pass |
| [fix_sonar_issues.md](.cursor/commands/fix_sonar_issues.md) | Static analysis fixes |
| [increase_coverage.md](.cursor/commands/increase_coverage.md) | Coverage loop |
| [review_code_structure.md](.cursor/commands/review_code_structure.md) | Architecture audit → `reports/` |

## Stack-specific workflow

**Squad order per task:** `T*n*-java` → `T*n*-htmx` → `T*n*-js` (if approved) → tests → next task.

**Full-stack surface:** update [feature-catalog.md](docs/feature-catalog.md) for new routes; update [`dev-import.sql`](src/main/resources/dev-import.sql) for happy-path seed; update [htmx-events.md](docs/htmx-events.md) when HTMX/auth patterns change.

**Finish gate:** squad sets `review-ready` → reviewers → **Review approval** → `GITHUB_ACTIONS=true ./mvnw -B verify`; `scripts/audit-template-seo.sh` when templates/SEO change.

**Tests:** never raw Selenium in test methods; extend `App`. Data setup via `Given`; call `Given.cleanup()` when the test mutates shared data.
