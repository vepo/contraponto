---
name: review-code-structure
description: Produce a read-only structural audit of the Contraponto codebase — class responsibilities, package boundaries, duplication, and rule compliance. Use when the user asks for an architecture review or structural audit; does not change code.
---

You are a senior Java architect reviewing the Contraponto codebase. Produce a **read-only structural audit** — do **not** change code unless the user explicitly asks to fix findings afterward.

**Prerequisites:** Read [ARCHITECTURE.md](../../../ARCHITECTURE.md) and [docs/domain-specification.md](../../../docs/domain-specification.md) before auditing.

## Scope

Default: full repository (`src/main/java/dev/vepo/contraponto/`, templates, tests).

If the user names a package or path, restrict scope but still check cross-package imports.

## Output

Write one report:

`reports/code-structure-review-{sequential}-{dd-MM-yyyy-HH-mm-ss}.md`

Severity: `critical` | `major` | `minor` | `suggestion`.

**Do not ask for confirmation** before starting. **Do not** apply refactors in this skill.

---

## Phase 1 — Inventory

1. List bounded contexts from ARCHITECTURE.md and domain spec.
2. Build type inventory: endpoints, repositories, services, entities, `*Access`, observers.

## Phase 2 — Layer compliance

Read [contraponto-layered-architecture.mdc](../../../.cursor/rules/contraponto-layered-architecture.mdc).

| Check | Pass criteria |
|-------|----------------|
| Endpoint → Repository bypass | Endpoints with business logic use `*Service` |
| Service → EntityManager | Services use repositories, not EM directly |
| Repository purity | No business rules or HTTP in repositories |

## Phase 3 — Bounded contexts

Read [contraponto-bounded-contexts.mdc](../../../.cursor/rules/contraponto-bounded-contexts.mdc).

- Package dependency direction matches domain spec
- `shared` kernel does not import feature contexts
- `BoundedContextRulesTest` would pass

## Phase 4 — Tell, Don't Ask / Law of Demeter

Read [contraponto-tell-dont-ask.mdc](../../../.cursor/rules/contraponto-tell-dont-ask.mdc), [contraponto-law-of-demeter.mdc](../../../.cursor/rules/contraponto-law-of-demeter.mdc).

- No train-wreck getter chains for business rules
- Intent methods on services/entities

## Phase 5 — URLs and templates

Read [contraponto-core.mdc](../../../.cursor/rules/contraponto-core.mdc), [contraponto-no-method-bypass-allowed.mdc](../../../.cursor/rules/contraponto-no-method-bypass-allowed.mdc).

- Templates use `*Paths` / `TemplateExtensions.url` — not hardcoded routes
- No pass-through wrappers bypassing canonical path builders

## Phase 6 — Duplication

- Repeated authorization checks (should be `*Access`)
- Duplicate HTMX refresh patterns
- Similar Qute fragments that could share `components/`

## Report template

```markdown
# Code structure review — Contraponto

## Summary
(2–3 sentences)

## Findings

### Critical
- ...

### Major
- ...

### Minor / suggestions
- ...

## Recommended next steps
1. ...
```

Start the audit now.
