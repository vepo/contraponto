# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Start here

This repo's conventions live in **[AGENTS.md](AGENTS.md)** — the master index for all AI-agent guidance — and in `.cursor/rules/*.mdc` (glob-scoped rule files Claude Code does not auto-load). Read `AGENTS.md` before non-trivial changes; it links to `ARCHITECTURE.md`, ADRs, domain spec, and the four always-on rule pillars (`domain-model.mdc`, `contraponto-testing.mdc`, `static-analysis.mdc`, `contraponto-core.mdc`). For Java style, imports, logging, and layering, also read `contraponto-java.mdc`, `contraponto-format-imports.mdc`, `contraponto-tell-dont-ask.mdc`, and `contraponto-law-of-demeter.mdc` in `.cursor/rules/`.

This repo also runs a seven-phase development process with role subagents (`.cursor/agents/`) requiring **explicit human approval** at ADR, task, and review gates — see the "Development process" section of `AGENTS.md`. Do not auto-accept ADRs, approve tasks, or mark work `done` on the user's behalf.

## Stack

Quarkus 3 (Java 25 — `pom.xml` is authoritative over the README's "Java 17+" badge) + Qute templates + HTMX. No SPA framework, no Node build. PostgreSQL/Hibernate/Flyway, Redis (prod sessions). Single Maven module, standard layout.

## Commands

Always use the `./mvnw` wrapper, not a bare `mvn`.

- Dev mode: `./mvnw quarkus:dev` (Flyway clean+migrate, seeds `dev-import.sql`)
- Full CI gate: `GITHUB_ACTIONS=true ./mvnw -B verify`
- Single class: `GITHUB_ACTIONS=true ./mvnw -B test -Dtest=ClassName`
- Single method: `GITHUB_ACTIONS=true ./mvnw -B test -Dtest=ClassName#methodName`
- Fast unit-only: `GITHUB_ACTIONS=true ./mvnw -B test -Ptest-unit` (also `-Ptest-quarkus`, `-Ptest-web`)
- Format check / fix: `./mvnw -B spotless:check` / `./mvnw spotless:apply`

**`GITHUB_ACTIONS=true` is required** for any test run — `@WebTest` uses it to run Selenium/Chrome headless. Without it, tests may open a visible browser locally and behave differently than CI.

## Gotchas

- Outbound ActivityPub HTTP Signature delivery needs the JVM flag `-Djdk.httpclient.allowRestrictedHeaders=host`. Maven (`quarkus:dev`, Surefire/Failsafe) and the Docker image already set it; add it yourself if launching outside Maven (e.g. a raw IDE run config).
- Every schema or feature change must also update the seed data in `src/main/resources/dev-import.sql` — see `dev-import-sql-safety.mdc`.
- Class members are auto-sorted and code is auto-formatted by Spotless (`resources/formatter.xml`, 4-space indent) as part of `compile`/`test`/`verify`. CI's `code-style` job runs `spotless:check` standalone (no prior build phase), so it fails on unformatted code even if a full local `verify` would have fixed it silently — run `./mvnw spotless:apply` before finishing if you haven't run a full build.
- Never call a method inside a log statement's arguments in production code — assign to a local first (SLF4J args are always evaluated).
- No pass-through/wrapper methods around canonical builders (e.g. `*Paths` classes) — call them directly.

## Commit style

Imperative present tense, capitalized subject (`Implement`, `Enhance`, `Refactor`, `Add`, `Change`), no conventional-commits prefix, no issue numbers.

## Skills

`.claude/skills/` has `/fix-tests`, `/fix-sonar-issues`, `/increase-coverage`, and `/review-code-structure` — Claude Code equivalents of this repo's `.cursor/commands/*.md`.
