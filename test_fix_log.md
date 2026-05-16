# Test fix log

## 2026-05-15

- Ran `mvn clean test` from project root.
- Result: **BUILD SUCCESS** — 91 tests, 0 failures, 0 errors, 0 skipped.
- No failing tests; no production or test code changes were required.

### GitHub Actions: `ElementClickIntercepted`

- **Cause:** Headless Chrome’s default viewport is small; fixed header/footer and dropdowns overlapped targets; many page-object methods used raw `WebElement.click()` instead of `reliableClick`.
- **Changes:**
  - `WebTestExtension`: `--window-size=1920,1080` for all Chrome runs.
  - `App`: route essentially all UI clicks through `reliableClick` (scroll-into-view + native click + JS click fallback); `_logout` / `goToReview` use `reliableClick` instead of scroll + bare click.
- **Verification:** `GITHUB_ACTIONS=true mvn clean test` — 91 tests pass.
