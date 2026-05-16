---
name: Fix All Tests
description: Automatically fix all failing Maven tests by iterating until they pass.
---

You are an expert Java developer. Your task is to fix **all failing tests** in this Maven project. Follow this exact loop:

1. **Run all tests**  
   Execute `mvn clean test` and capture the full output.

2. **Check for failures**  
   - If the build succeeds and there are **no test failures**, print `✅ All tests pass!` and stop.  
   - If there are failures, proceed to step 3.

3. **List each failing test with the reason**  
   Parse the surefire reports (in `target/surefire-reports/*.txt`) or the console output. For each failure, output:
   - Test class & method name
   - Exception type and stack trace
   - Assertion error details (expected vs actual, etc.)

4. **Fix each failing test** (one at a time, in the order listed)  
   For each failure:
   - Read the test code and the production code it tests.
   - Determine the root cause: logic error, incorrect assertion, missing mock, setup issue, etc.
   - **Apply a fix** by editing the relevant Java files (test or main). Prefer changing production code only if the test correctly identifies a bug; otherwise fix the test.
   - After each fix, **re-run that single test** to verify (`mvn test -Dtest=TestClassName#methodName`). If it passes, move to the next failure.

5. **Repeat from step 1**  
   After fixing all failures identified in the current iteration, run `mvn clean test` again. If new failures appear (or old ones persist), loop back to step 2.

**Important rules:**
- Do **not** ask for confirmation before editing files or running commands.
- Do **not** skip any test failure.
- If a fix requires multiple attempts (e.g., first attempt fails), try a different strategy (e.g., fix production code instead of test).
- Keep a log of changes made in `test_fix_log.md`.

Start the loop now.