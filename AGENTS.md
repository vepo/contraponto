# Agent instructions (Contraponto)

Read these before changing code or tests:

| Document | Purpose |
|----------|---------|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Stack, patterns, URL map, schema, feature workflows |
| [docs/htmx-events.md](docs/htmx-events.md) | HTMX lifecycle, custom events, scoped refresh, auth allowlist |
| [docs/conventions-checklist.md](docs/conventions-checklist.md) | Gaps between docs and code; doc debt to close |
| [docs/application-guidelines.md](docs/application-guidelines.md) | Route-level UX flows (update when routes change) |
| [docs/feature-catalog.md](docs/feature-catalog.md) | UI feature index with step counts and navigation paths (update when routes/menu change) |
| [docs/ui-guidelines.md](docs/ui-guidelines.md) | Visual design system (UX, page flows) |
| [docs/ui-elements.md](docs/ui-elements.md) | CSS class catalog and main/manage/write bundles |
| [docs/git-jekyll-convention.md](docs/git-jekyll-convention.md) | Git ↔ Jekyll sync layout |
| [docs/deployment.md](docs/deployment.md) | Production checklist (DB, SMTP, URL, secrets) |
| [.cursor/rules/](.cursor/rules/) | Cursor rules (always-on + file-scoped); list UIs → `contraponto-pagination.mdc` |

**Workflow:** entity/repository → service (if non-trivial) → endpoint → Qute template → `@WebTest` with `App` + `Given` → navigation/links if user-facing.

**Tests:** never raw Selenium in test methods; extend `App`. Data setup via `Given`; call `Given.cleanup()` when the test mutates shared data.
