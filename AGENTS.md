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

**Baseline ADRs (Accepted):** [ADR-0002](docs/adr/0002-backend-java-quarkus-jakarta-ee.md) Java + Quarkus; [ADR-0003](docs/adr/0003-frontend-qute-htmx.md) Qute + HTMX; [ADR-0004](docs/adr/0004-package-naming-dev-vepo.md) `dev.vepo.contraponto`; [ADR-0005](docs/adr/0005-postgresql-database.md) PostgreSQL + Flyway; [ADR-0006](docs/adr/0006-activitypub-federation.md)–[0008](docs/adr/0008-activitypub-actor-identity.md) ActivityPub federation.

**ActivityPub feature:** [feature/activitypub-integration.md](feature/activitypub-integration.md) — **done** (T1–T13; manual Mastodon interop checklist remains).

**Development process:** [development-process.mdc](.cursor/rules/development-process.mdc) — five phases. **Manual acceptance only:** ADRs → explicit user message; features → explicit task IDs (phase 4). Agents must not auto-accept ADRs or start coding without approval.

## Agents vs commands

| Surface | Location | Purpose |
|---------|----------|---------|
| **Subagents** | `.cursor/agents/*.md` | TDD red/green/refactor, domain modeling, docs sync |
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

## Project subagents

| Subagent | When to delegate |
|----------|------------------|
| [tdd-red](.cursor/agents/tdd-red.md) | Failing test only |
| [tdd-green](.cursor/agents/tdd-green.md) | Minimal code to pass |
| [tdd-refactor](.cursor/agents/tdd-refactor.md) | Refactor with tests green |
| [domain-model](.cursor/agents/domain-model.md) | Domain-spec and vocabulary |
| [docs-sync](.cursor/agents/docs-sync.md) | ADRs, feature catalog, ARCHITECTURE |

**TDD cycle (phase 5 only):** analysis → architecture + ADRs → tasks → approval → red → green → refactor.

## Commands

| Command | Purpose |
|---------|---------|
| [fix_tests.md](.cursor/commands/fix_tests.md) | Loop until tests pass |
| [fix_sonar_issues.md](.cursor/commands/fix_sonar_issues.md) | Static analysis fixes |
| [increase_coverage.md](.cursor/commands/increase_coverage.md) | Coverage loop |
| [review_code_structure.md](.cursor/commands/review_code_structure.md) | Architecture audit → `reports/` |

## Stack-specific workflow

**Backend:** entity → repository → service (if non-trivial) → endpoint → Qute template → `@WebTest` with `App` + `Given`.

**Full-stack surface:** update [feature-catalog.md](docs/feature-catalog.md) for new routes; update [`dev-import.sql`](src/main/resources/dev-import.sql) for happy-path seed.

**Finish gate:** `GITHUB_ACTIONS=true ./mvnw -B verify`; `scripts/audit-template-seo.sh` when templates/SEO change; `dev-import.sql` + coverage registry when schema or feature behaviour changed.

**Tests:** never raw Selenium in test methods; extend `App`. Data setup via `Given`; call `Given.cleanup()` when the test mutates shared data.
