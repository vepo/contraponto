# Agent instructions (Contraponto)

Read these before changing code or tests:

| Document | Purpose |
|----------|---------|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Stack, patterns, URL map, schema, feature workflows |
| [docs/HTMX-Events.md](docs/HTMX-Events.md) | HTMX lifecycle, custom events, scoped refresh, auth allowlist |
| [docs/CONVENTIONS_CHECKLIST.md](docs/CONVENTIONS_CHECKLIST.md) | Gaps between docs and code; doc debt to close |
| [docs/Application-Guidelines.md](docs/Application-Guidelines.md) | Route-level UX flows (update when routes change) |
| [docs/UI-Guidelines.md](docs/UI-Guidelines.md) | Visual design system |
| [docs/git-jekyll-convention.md](docs/git-jekyll-convention.md) | Git ↔ Jekyll sync layout |
| [.cursor/rules/](.cursor/rules/) | Cursor rules (always-on + file-scoped); list UIs → `contraponto-pagination.mdc` |

**Workflow:** entity/repository → service (if non-trivial) → endpoint → Qute template → `@WebTest` with `App` + `Given` → navigation/links if user-facing.

**Tests:** never raw Selenium in test methods; extend `App`. Data setup via `Given`; call `Given.cleanup()` when the test mutates shared data.
