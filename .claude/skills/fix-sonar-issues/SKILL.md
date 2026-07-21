---
name: fix-sonar-issues
description: Run local static analysis (no SonarCloud token) on this Quarkus/Java project and fix findings with conservative, behavior-preserving changes. Use when the user asks to fix static analysis issues, lint warnings, or Sonar-style findings without calling SonarCloud.
disable-model-invocation: true
---

You are an expert Java developer working on Contraponto. Your task is to fix **all issues surfaced by local static analysis** — the same class of problems SonarCloud flags, without calling SonarCloud or using any token. Work **slowly and conservatively**; a clean local report must never come at the cost of broken behavior, weaker security, or masked bugs.

Align with [static-analysis.mdc](../../../.cursor/rules/static-analysis.mdc): **local checks only** (no SonarCloud, tokens, or upload services).

**Test runs:** Always prefix Maven with `GITHUB_ACTIONS=true` so `@WebTest` Chrome runs headless (matches CI).

Follow this exact loop — **do not ask for confirmation** before editing, but **do stop and report** if a fix would change observable behavior and the correct behavior is unclear.

## 1. Discover issues (local only)

Fast pass (formatting + compile warnings):

```bash
GITHUB_ACTIONS=true ./mvnw -B spotless:check
GITHUB_ACTIONS=true ./mvnw -B compile -Dmaven.compiler.showWarnings=true -Dmaven.compiler.showDeprecation=true
```

Then scan **IDE diagnostics** on production and test Java:

- Run `ReadLints` on `src/main/java` and `src/test/java` (or on files you touch each iteration).
- Treat compiler warnings, Spotless violations, and linter warnings as the work queue.
- Review obvious smells while reading flagged files: empty catches, `System.out`, `printStackTrace`, broad `@SuppressWarnings`, `NOSONAR`.

Do **not** call SonarCloud API or `sonar-maven-plugin:sonar`. Do **not** ask for `SONAR_TOKEN`.

## 2. Prioritize

Process findings in this order:

1. **Hard failures:** compile errors, Spotless violations, failing tests from `verify`.
2. **Reliability / security smells:** empty catches, swallowed exceptions, `System.out`, `printStackTrace`.
3. **Compiler + linter warnings:** deprecations, unused code, null-safety, raw types.
4. **Maintainability:** cognitive complexity (large methods), duplicated literals (`java:S1192`-style), broad `@SuppressWarnings`, `NOSONAR`.
5. **Scope:** single-file, localized fixes before cross-cutting refactors.
6. **Rule clusters:** batch only when the same smell hits the same file with identical fix pattern (e.g. duplicated JPQL parameter names in one repository).

Skip **won't-fix / false-positive** candidates only after you can justify them in the log (see step 6). Never bulk-`@SuppressWarnings` or `//NOSONAR`.

## 3. Before touching code

For each finding (or tight cluster):

1. Read the **file and surrounding class** — match patterns in sibling code ([contraponto-java.mdc](../../../.cursor/rules/contraponto-java.mdc), [contraponto-layered-architecture.mdc](../../../.cursor/rules/contraponto-layered-architecture.mdc)).
2. If the issue touches HTTP, auth, or templates → skim [ARCHITECTURE.md](../../../ARCHITECTURE.md) and the co-located Qute template.
3. If the issue touches domain rules → skim [docs/domain-specification.md](../../../docs/domain-specification.md) Ubiquitous Language.
4. Understand **why** the rule flags it; plan the smallest fix that addresses the root cause.

## 4. Fix strategies (prefer refactor over suppression)

| Rule / theme | Safe approach in this repo | Avoid |
|---|---|---|
| `java:S1192` duplicated string literals | `private static final String PARAM_*` for JPQL named parameters (see `BlogRepository`, `PostRepository`) | Suppressing on whole class unless the literal is a domain constant already centralized |
| Cognitive complexity / brain method | Extract **private** methods with clear domain names; keep endpoint/service contracts unchanged | Splitting across layers incorrectly (business logic into endpoint just to lower count) |
| Too many returns / nested if | Early returns and extracted validators (see `BlogSaveEndpoint.validateSlug`) | Duplicating validation paths |
| Regex DoS (`java:S5852` etc.) | Possessive quantifiers + comment explaining malformed input (see `ContentRenderTagProcessor`) | Weaker regex that accepts invalid input |
| Missing template parameters (Qute) | Pass explicit view-model fields (`Links`, `LoggedUser`, …) into `@CheckedTemplate` methods | `@Inject` static fields into templates |
| Type name not PascalCase (`java:S101`) | Rename class/record/interface/enum to `^[A-Z][a-zA-Z0-9]*$`; MailTemplate records must match `templates/{OwnerClass}/{RecordName}.html` | Lowercase record names to match legacy Qute paths |
| Unused imports / dead code | Remove only after confirming zero references (endpoints, CDI, tests, templates) | Deleting beans looked up via `Arc` or framework callbacks |
| Exception handling | Log with SLF4J + rethrow or map to domain `Response`; never empty catch | `catch (Exception ignored)` |
| Test smells | Fix assertion or test data; keep production behavior | `@Disabled`, `@Ignore`, weakened assertions |
| `@SuppressWarnings("unchecked")` on JPA `createQuery` | Keep narrow scope on the single statement when Criteria/tuple typing requires it | Class-level suppression |
| Spotless / formatter drift | `./mvnw spotless:apply` then re-check | Hand-editing formatting outside the formatter |

**Never:**

- Add `//NOSONAR` or broad `@SuppressWarnings` to silence without fixing.
- Change public URLs, form field names, or auth gates to appease static analysis.
- Weaken null checks, validation, or transaction boundaries.
- "Fix" by deleting tests or excluding files in `pom.xml`.

## 5. Verify each batch

After every finding (or same-rule cluster in one file):

```bash
GITHUB_ACTIONS=true ./mvnw -B spotless:check
GITHUB_ACTIONS=true ./mvnw -B compile -Dmaven.compiler.showWarnings=true -Dmaven.compiler.showDeprecation=true
```

When the fast pass is clean, run the full CI gate:

```bash
GITHUB_ACTIONS=true ./mvnw -B verify
```

If Spotless fails: `./mvnw spotless:apply` then re-run checks.

If a test fails:

- Fix the **root cause** (production bug or outdated test) — same discipline as the `fix-tests` skill.
- Do **not** proceed to the next issue until `verify` is green.

Re-run `GITHUB_ACTIONS=true ./mvnw -B verify` after large batches or before declaring done.

## 6. Log every change

Append to `reports/sonar_fix_log-{sequential}-{dd-MM-yyyy-HH-mm-ss}.md`:

- Source (compiler / linter / test failure)
- Rule or category / file:line / message
- Root cause (one sentence)
- Fix strategy
- Files touched
- `verify` result
- If suppressed or marked false positive: explicit justification

## 7. Stop condition

When **all** of the following are clean:

- `GITHUB_ACTIONS=true ./mvnw -B spotless:check`
- `GITHUB_ACTIONS=true ./mvnw -B compile` with no warnings you can fix
- `ReadLints` on `src/main/java` and `src/test/java` — no unresolved errors; fix relevant warnings
- `GITHUB_ACTIONS=true ./mvnw -B verify`

print:

`✅ Local static analysis clean!`

and summarize: issues fixed, any deferred items, final `verify` status.

Note: CI still uploads to SonarCloud separately (`.github/workflows/`). This skill does not require or wait on that upload.

## 8. If stuck

Stop the loop and report:

- Finding category + message + file
- Why a safe fix is unclear (behavior, architecture, missing test coverage)
- Two concrete options for the user to choose

Do **not** apply a risky workaround.

Start the loop now.
