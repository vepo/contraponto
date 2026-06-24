# GitHub Actions — Contraponto

Workflow: [`ci.yml`](ci.yml) — **Continuous Integration**

## Pipeline overview

| Job | Purpose |
|-----|---------|
| **Code Style · Spotless** | Fails fast on formatting drift (`mvn spotless:check`) |
| **Tests · Unit / Quarkus Integration / Web** | Parallel Maven Surefire shards (`-Ptest-unit`, `-Ptest-quarkus`, `-Ptest-web`) |
| **Quality · Test Results Summary** | Publishes JUnit XML to GitHub Checks and PR comments on failures |
| **Build · JVM Package & Static Assets** | `mvn package -DskipTests` — minify gate for JS/CSS |
| **Quality · SonarCloud Analysis** | Merges JaCoCo from test shards; SonarCloud scan (skipped on fork PRs) |
| **Release · Docker Hub (JVM)** | Pushes `vepo/contraponto` on `main` and tags only |

Run title format: `CI · <branch> · <sha>`

## Stored artifacts

Artifacts are downloadable from the **Actions → workflow run → Artifacts** panel.

| Artifact | Produced by | Retention | Contents | When to download |
|----------|-------------|-----------|----------|------------------|
| `test-results-unit` | Tests · Unit | 30 days | Surefire `*.xml` + `*.txt` | Investigate unit test failures |
| `test-results-quarkus` | Tests · Quarkus Integration | 30 days | Surefire reports | Integration / `@QuarkusTest` failures |
| `test-results-web` | Tests · Web | 30 days | Surefire reports | `@WebTest` / Selenium failures |
| `jacoco-unit` / `jacoco-quarkus` / `jacoco-web` | Test shards | 1 day | `jacoco-quarkus.exec` | Internal — consumed by Sonar job |
| `coverage-report` | SonarCloud job | 30 days | JaCoCo HTML + `jacoco.xml` | Local coverage review without Sonar UI |
| `build-quarkus-app` | Build job | 7 days | `target/quarkus-app/` | Reproduce Docker layer or deploy without rebuild |

**Not stored as artifacts (by design):**

- **Docker images** — pushed to Docker Hub (`vepo/contraponto:main`, semver tags, `sha-*`)
- **SonarCloud report** — viewed in [SonarCloud](https://sonarcloud.io) (requires project access)

### Test results on GitHub

Yes — **JUnit XML from Maven Surefire is stored and published.**

1. Each test shard uploads `target/surefire-reports/**/*.xml` (and `.txt` logs).
2. **Quality · Test Results Summary** merges all shards and publishes a **GitHub Check** named `Maven · All Test Suites`.
3. On failed PRs, a comment summarizes failing tests (`comment_mode: failures`).

Open the check on a PR for per-suite and per-test failure details without downloading artifacts.

## Secrets

| Secret | Used by |
|--------|---------|
| `SONAR_TOKEN` | SonarCloud analysis |
| `DOCKERHUB_USERNAME` / `DOCKERHUB_TOKEN` | Container release |

## Local parity

```bash
GITHUB_ACTIONS=true mvn -B spotless:check
GITHUB_ACTIONS=true mvn -B test -Ptest-unit    # or test-quarkus / test-web
GITHUB_ACTIONS=true mvn -B verify              # full gate before merge
```

See [`.cursor/rules/static-analysis.mdc`](../.cursor/rules/static-analysis.mdc) and [`.cursor/rules/test-headless-github-actions.mdc`](../.cursor/rules/test-headless-github-actions.mdc).
