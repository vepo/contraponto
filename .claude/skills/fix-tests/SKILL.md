---
name: fix-tests
description: Automatically fix all failing Maven tests in this Quarkus/Java project by iterating until they pass. Use when the user asks to fix failing tests or get the test suite green.
disable-model-invocation: true
---

You are an expert Java developer. Your task is to fix **all failing tests** in this Maven project.
Follow this exact loop — **do not ask for confirmation** and **do not invent workarounds**.

**Test runs:** Always set `GITHUB_ACTIONS=true` when invoking Maven tests (e.g. `GITHUB_ACTIONS=true ./mvnw clean test`). `WebTestExtension` uses this to run Chrome headless, which matches CI and avoids failures when no display is available.

## Test tags (parallel execution)

Tests are grouped with JUnit 5 tags (`unit`, `quarkus`, `web`) via `@UnitTest`, `@QuarkusIntegrationTest`, and `@WebTest` in `dev.vepo.contraponto.shared`.

| Tag | Profile | Scope |
|-----|---------|-------|
| `unit` | `-Ptest-unit` | Plain JUnit tests (no Quarkus boot) |
| `quarkus` | `-Ptest-quarkus` | `@QuarkusIntegrationTest` HTTP/DB integration tests |
| `web` | `-Ptest-web` | `@WebTest` browser tests (Chrome headless) |

- **`./mvnw clean test`** (no profile) runs **all tags** sequentially in one JVM.
- **Parallel run** (same as CI): compile once, then run each tag in a separate Maven process:

```bash
GITHUB_ACTIONS=true ./mvnw -B clean test-compile
GITHUB_ACTIONS=true ./mvnw -B test -Ptest-unit &
GITHUB_ACTIONS=true ./mvnw -B test -Ptest-quarkus &
GITHUB_ACTIONS=true ./mvnw -B test -Ptest-web &
wait
```

Do **not** run `./mvnw clean` in parallel — it races on `target/`. After code changes, run `./mvnw test-compile` once, then repeat the parallel `test -P…` block.

Surefire reports for parallel runs land in the same `target/surefire-reports/` directory; parse all `*.txt` files there after `wait`.

1. **Run all tests (parallel)**
   Execute the parallel block above and capture output from all three processes.

2. **Check for failures**
   - If every process exits 0 and there are **no test failures**, print `✅ All tests pass!` and stop.
   - If any process fails, proceed to step 3.

3. **List each failing test with the reason**
   Parse the Surefire reports in `target/surefire-reports/*.txt` or the console output.
   For each failure, output:
   - Test class & method name
   - Tag (`unit`, `quarkus`, or `web`) if known from the class annotation
   - Exception type and stack trace
   - Assertion error details (expected vs actual, etc.)

4. **Fix each failing test** (one at a time, in the order listed)
   For each failure:
   - Read `ARCHITECTURE.md` to remember the architecture
   - Read the **test code** and the **production code** it calls.
   - Determine the root cause: logic error in production code, incorrect assertion, missing mock, wrong setup, etc.
   - **Apply a direct fix**:
     - If the production code is wrong → fix the production code.
     - If the test is wrong (e.g., outdated expectation, wrong mock) → fix the test.
     - **Never** add code that works around the failure without addressing the real cause (e.g., do not add `Thread.sleep()` to hide timing issues, do not ignore exceptions).
   - After the fix, **re-run that single test** to verify:
     `GITHUB_ACTIONS=true ./mvnw test -Dtest=TestClassName#methodName`
   - If it still fails, try a different fix strategy (e.g., change the production logic instead of the test).

5. **Repeat from step 1**
   After fixing all failures identified in the current iteration, run the parallel test block again (with `test-compile` if sources changed).
   - If new failures appear (or old ones persist), loop back to step 2.
   - If no failures → stop and report success.

**Important rules:**
- Do **not** ask for confirmation before editing files or running commands.
- Do **not** skip any test failure.
- Do **not** introduce workarounds or temporary fixes (e.g., `@Ignore`, try-catch swallowing exceptions, adding delays).
- Keep a log of every change in `reports/test_fix_log-{sequential number}-{dd-MM-yyyy-HH-mm-ss}.md` (file path, old code, new code, reason).

Start the loop now.
