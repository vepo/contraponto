---
name: Fix All Tests
description: Automatically fix all failing Maven tests by iterating until they pass (generic Java).
---

You are an expert Java developer. Your task is to fix **all failing tests** in this Maven project.  
Follow this exact loop – **do not ask for confirmation** and **do not invent workarounds**.

**Test runs:** Always set `GITHUB_ACTIONS=true` when invoking Maven tests (e.g. `GITHUB_ACTIONS=true mvn clean test`). `WebTestExtension` uses this to run Chrome headless, which matches CI and avoids failures when no display is available.

1. **Run all tests**  
   Execute `GITHUB_ACTIONS=true mvn clean test` and capture the full output.

2. **Check for failures**  
   - If the build succeeds and there are **no test failures**, print `✅ All tests pass!` and stop.  
   - If there are failures, proceed to step 3.

3. **List each failing test with the reason**  
   Parse the Surefire reports in `target/surefire-reports/*.txt` or the console output.  
   For each failure, output:
   - Test class & method name  
   - Exception type and stack trace  
   - Assertion error details (expected vs actual, etc.)  

4. **Fix each failing test** (one at a time, in the order listed)  
   For each failure:
   - Read `ARCHITECTURE.md` to remember the Architecture
   - Read the **test code** and the **production code** it calls.  
   - Determine the root cause: logic error in production code, incorrect assertion, missing mock, wrong setup, etc.  
   - **Apply a direct fix**:  
     - If the production code is wrong → fix the production code.  
     - If the test is wrong (e.g., outdated expectation, wrong mock) → fix the test.  
     - **Never** add code that works around the failure without addressing the real cause (e.g., do not add `Thread.sleep()` to hide timing issues, do not ignore exceptions).  
   - After the fix, **re-run that single test** to verify:  
     `GITHUB_ACTIONS=true mvn test -Dtest=TestClassName#methodName`  
   - If it still fails, try a different fix strategy (e.g., change the production logic instead of the test).  

5. **Repeat from step 1**  
   After fixing all failures identified in the current iteration, run `GITHUB_ACTIONS=true mvn clean test` again.  
   - If new failures appear (or old ones persist), loop back to step 2.  
   - If no failures → stop and report success.

**Important rules:**
- Do **not** ask for confirmation before editing files or running commands.
- Do **not** skip any test failure.
- Do **not** introduce workarounds or temporary fixes (e.g., `@Ignore`, try-catch swallowing exceptions, adding delays).
- Keep a log of every change in `reports/test_fix_log-{sequential number}-{dd-MM-yyyy-HH-mm-ss}.md` (file path, old code, new code, reason).

Start the loop now.