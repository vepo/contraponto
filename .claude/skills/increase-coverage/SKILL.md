---
name: increase-coverage
description: Automatically raise JaCoCo test coverage to 80% (instruction and branch) in this Quarkus/Java project while ensuring every test asserts all results. Use when the user asks to increase test coverage or close coverage gaps.
disable-model-invocation: true
---

You are an expert Java test engineer. Your goal: raise **both** JaCoCo **instruction** and **branch** coverage to **≥ 80%** (or thresholds defined in `pom.xml`) and enforce that **every test method contains assertions for all returned values and state changes**.

Follow this exact loop:

**Test runs:** Always set `GITHUB_ACTIONS=true` when invoking Maven tests (e.g. `GITHUB_ACTIONS=true ./mvnw clean test`). `WebTestExtension` uses this to run Chrome headless, which matches CI and avoids failures when no display is available.

1. **Measure current coverage**
   Run `GITHUB_ACTIONS=true ./mvnw clean test` then `./mvnw jacoco:report` (JaCoCo is configured in `pom.xml`; report at `target/jacoco-report/jacoco.xml` and HTML under `target/jacoco-report/`).
   If JaCoCo is missing, add it to `pom.xml` (under `build/plugins`) with:
   ```xml
   <plugin>
       <groupId>org.jacoco</groupId>
       <artifactId>jacoco-maven-plugin</artifactId>
       <version>0.8.14</version>
       <executions>
           <execution>
               <goals>
                   <goal>prepare-agent</goal>
                   <goal>report</goal>
               </goals>
           </execution>
       </executions>
   </plugin>
   ```

2. **Check coverage thresholds**
   Parse the JaCoCo XML report (`target/jacoco-report/jacoco.xml`). Read **report-level** counters for:
   - `INSTRUCTION`
   - `BRANCH` (required — do not stop on instruction alone)

   Print both percentages in the log every iteration.

   **Stop when both are satisfied:**
   - `INSTRUCTION` coverage **≥ 80%**
   - `BRANCH` coverage **≥ 80%**

   Then print `✅ Coverage target reached! (instruction and branch ≥ 80%)` and stop.

   **Otherwise**, build the work queue:
   - List the **5 lowest branch-coverage** classes (min 20 instructions, skip synthetic `$` types) with missed branch count.
   - Also list the **5 lowest instruction-coverage** classes if different.
   - **Prioritize branch gaps** when choosing what to test next (branch coverage is usually the binding constraint).
   - For each target class, note **uncovered branches** (from method-level `BRANCH` counters in the XML) and prefer tests that exercise `if/else`, `switch`, validation failures, and error paths — not only happy paths.

3. **For each low-coverage class** (branch-poor first, then instruction-poor):
   - Identify specific **uncovered lines and branches** from the report.
   - **Before writing tests**, check whether uncovered code is **genuinely unused** and can be removed safely (see **Removing unused code** below). If removal is justified, delete it, run tests, re-measure coverage, then move to the next class.
   - If no test exists for that class → create a new test class (`*Test.java`) in `src/test/java` with the same package structure.
   - If a test exists → **augment existing tests** to cover the missing **branches** (e.g. 400/403/404, empty input, duplicate action, non-owner, already-resolved state).
   - **Critical rule:** Every test method you write or modify must **assert every relevant result**:
     - For methods returning a value → assert the return value (equality, nullity, etc.).
     - For void methods → assert state changes (e.g., object fields, repository state after `entityManager.clear()`, HTTP status, response body fragments).
     - For error paths → assert status code **and** that persisted state did not change (or changed as expected).
     - Never leave a test method without at least one assertion. Never call a method and ignore its return value.
   - After modifying a test, **run that single test** (`GITHUB_ACTIONS=true ./mvnw test -Dtest=NewOrModifiedTest`) to confirm it passes.

4. **After covering one class** (its missed branches and lines addressed), run the full coverage measurement again (`GITHUB_ACTIONS=true ./mvnw clean test` then `./mvnw jacoco:report`).

5. **Repeat from step 2** until **instruction ≥ 80%** and **branch ≥ 80%**.

**Removing unused code (preferred over testing dead code):**

Only delete production code after you have **confirmed it is unreachable and unreferenced**. When in doubt, add tests instead of deleting.

1. **Establish it is unused**
   - Search the repo for the class, method, and field names (including string literals and FQCNs).
   - Trace call paths from endpoints (`@Path`), services used by endpoints, CDI observers (`@Observes`), and event publishers.
   - Check Qute templates (`.html` co-located with endpoints) and `TemplateExtensions` for static helpers that call into Java.
   - **Quarkus/CDI:** A bean with no direct inject sites may still be required — e.g. lookup via `Arc.container()`, `@Produces`, or framework callbacks. Do not remove such beans unless you also remove or refactor every lookup site. If a bean must stay but Arc would strip it, use `@io.quarkus.arc.Unremovable` only when the bean is actually needed at runtime (do not use `@Unremovable` to keep truly dead code).

2. **Safe to remove (examples)**
   - Private methods with no callers in `src/main` or `src/test`.
   - Unreachable branches after a guard that can never be true in the current domain (verify against `docs/domain-specification.md`).
   - Entire types with zero references and that are not registered as CDI beans, JAX-RS resources, Flyway/Java migration hooks, or extension points.

3. **Do not remove without explicit proof**
   - Public API on endpoints, entities, or DTOs consumed by templates or other modules.
   - Code referenced only from tests (fix or delete the test, not production, unless the production path is obsolete).
   - "Unused" imports, fields, or methods that exist for future features unless the team clearly abandoned them — prefer a minimal test or a documented issue over silent deletion.

4. **After each removal**
   - Run `GITHUB_ACTIONS=true ./mvnw clean test` (full suite, not only the class you touched).
   - Re-run coverage and confirm **instruction and branch** improved or stayed the same.
   - Record what was removed and how you verified it in `reports/coverage_log-{sequential number}.md`.

**Additional rules:**
- Do **not** delete or weaken existing assertions.
- Prefer **safe removal of unused code**, then adding new test methods; only change production behavior when fixing a clear bug or when removal is verified as above.
- If a method is unreachable due to design issues (private methods, complex dependencies), first consider deletion if truly dead; otherwise use reflection, or refactor the production code to be testable (e.g., extract logic, add package-private access).
- Keep a log of changes in `reports/coverage_log-{sequential number}-{dd-MM-yyyy-HH-mm-ss}.md`. Each entry must record **instruction %** and **branch %** before and after.

**Quick parse snippet (optional):** use report-level and class-level `BRANCH` counters in `jacoco.xml`; sort classes by ascending branch % where `missed + covered > 0`.

Start the loop now.
