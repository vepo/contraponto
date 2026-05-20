# Contributing to Contraponto

Thank you for your interest in contributing. This project is a Quarkus + Qute + HTMX blog platform; start with [README.md](README.md) and [ARCHITECTURE.md](ARCHITECTURE.md).

## Development setup

1. **JDK 25** and **Maven** (or use the Maven wrapper when present).
2. **PostgreSQL** — dev mode uses Testcontainers for tests; local dev uses Flyway + `dev-import.sql` on startup.
3. **Chrome** — required for `@WebTest` (Selenium).

```bash
mvn quarkus:dev
# or: ./mvnw quarkus:dev
```

Open [http://localhost:8080](http://localhost:8080). Dev personas and passwords are documented in [`.cursor/rules/development-experience.mdc`](.cursor/rules/development-experience.mdc).

## Workflow

1. Fork and branch from `main`.
2. For domain or UI changes, read [docs/domain-specification.md](docs/domain-specification.md) first and keep ubiquitous language aligned.
3. Follow the feature workflow in ARCHITECTURE.md §9: entity → repository → service (if needed) → endpoint → template → test → navigation/seed data.
4. Run tests before opening a PR:

```bash
mvn test
# full verify (unit + integration):
mvn verify
```

5. Open a pull request using the PR template checklist.

## Tests

- Use the `App` DSL and `Given` builders — do not call Selenium directly in test methods.
- Call `Given.cleanup()` when a test mutates shared data.
- See [AGENTS.md](AGENTS.md) and `.cursor/rules/contraponto-tests.mdc` for conventions.

## Documentation

| Change | Update |
|--------|--------|
| New/changed routes or menu | [docs/feature-catalog.md](docs/feature-catalog.md), [dev-import.sql](src/main/resources/dev-import.sql) |
| Domain concepts or UI labels | [docs/domain-specification.md](docs/domain-specification.md) |
| Deploy / ops | [docs/deployment.md](docs/deployment.md) |

## Code style

Formatting is enforced via Spotless and the Eclipse formatter (`mvn spotless:apply` / build phase). Match patterns in the package you touch.

## Security

See [.github/SECURITY.md](.github/SECURITY.md) for reporting vulnerabilities.

## License

By contributing, you agree that your contributions are licensed under the same terms as the project ([LICENSE](LICENSE) — GPLv2).
