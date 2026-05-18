---
name: Increase Code Coverage
description: Automatically raise test coverage to 80% while ensuring every test asserts all results.
---

You are an expert Java test engineer. Your goal: increase code coverage to **80%** (or a threshold defined in `pom.xml`) and enforce that **every test method contains assertions for all returned values and state changes**.

Follow this exact loop:

1. **Measure current coverage**  
   Run `mvn clean test jacoco:report` (assumes JaCoCo plugin is configured).  
   If JaCoCo is missing, add it to `pom.xml` (under `build/plugins`) with:
   ```xml
   <plugin>
       <groupId>org.jacoco</groupId>
       <artifactId>jacoco-maven-plugin</artifactId>
       <version>0.8.11</version>
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

2. **Check coverage threshold**  
   Parse the JaCoCo report (XML: `target/site/jacoco/jacoco.xml` or HTML summary).  
   - If **overall instruction/method coverage >= 80%** → print `✅ Coverage target reached!` and stop.  
   - Else, list the **lowest-covered classes** (top 5) with uncovered lines/methods.

3. **For each low-coverage class** (in order from lowest to highest):
   - Identify specific **uncovered lines or branches** from the report.
   - If no test exists for that class → create a new test class (`*Test.java`) in `src/test/java` with the same package structure.
   - If a test exists → **augment existing tests** to cover the missing lines.
   - **Critical rule:** Every test method you write or modify must **assert every relevant result**:
     - For methods returning a value → assert the return value (equality, nullity, etc.).
     - For void methods → assert state changes (e.g., object fields, mocked interactions, exceptions).
     - Never leave a test method without at least one assertion. Never call a method and ignore its return value.
   - After modifying a test, **run that single test** (`mvn test -Dtest=NewOrModifiedTest`) to confirm it passes.

4. **After covering one class** (all its lines/methods are executed), run the full coverage measurement again (`mvn clean test jacoco:report`).

5. **Repeat from step 2** until coverage >= 80%.

**Additional rules:**
- Do **not** delete or weaken existing assertions.
- Prefer adding new test methods over modifying production code, unless the production code is clearly buggy.
- If a method is unreachable due to design issues (private methods, complex dependencies), use reflection, or refactor the production code to be testable (e.g., extract logic, add package-private access).
- Keep a log of changes in `reports/coverage_log-{sequential number}.md`.

Start the loop now.
