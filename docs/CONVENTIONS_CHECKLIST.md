# Conventions checklist

Tracks what is **documented and enforced** vs **implemented but under-documented** vs **missing as a team convention**. Update this when closing gaps.

Legend: ✅ done · ⚠️ partial · ❌ missing / outdated

## Documentation map

| Area | ARCHITECTURE | Application-Guidelines | UI-Guidelines | Cursor rules |
|------|:------------:|:----------------------:|:-------------:|:------------:|
| Multi-blog URLs | ✅ | ⚠️ | — | ✅ |
| Post publications / versions | ✅ | ❌ | — | ✅ |
| Tags & series | ✅ | ❌ | ⚠️ | ⚠️ |
| Notifications & audience | ✅ | ❌ | ❌ | ✅ |
| Git/Jekyll sync | ✅ (link) | ❌ | — | ✅ |
| RSS feeds | ✅ | ❌ | — | — |
| Custom page URL shapes | ✅ | ⚠️ (old `/page/{username}/...`) | — | ✅ |
| Roles (`USER_ADMINISTRATOR`) | ✅ | ❌ | — | ✅ |
| User/blog admin UI | ✅ | ❌ | — | — |
| HTMX events / scoped refresh | ✅ | — | — | — |

## Conventions to adopt or document

### High priority (code exists, docs lag)

- [ ] **Update [Application-Guidelines.md](Application-Guidelines.md)** — notifications, tags, series, publications, `/users`, `/blogs`, `/pages`, RSS, git, corrected custom-page URLs (`CustomPagePaths`).
- [ ] **Update [UI-Guidelines.md](UI-Guidelines.md)** — notification bell/inbox, follow/subscribe controls on posts, tag/serie surfaces, version history UI.
- [ ] **Service layer guideline** — when logic belongs in `XxxService` vs endpoint vs repository (ARCHITECTURE §5 is a start; add examples per package).
- [ ] **CDI events catalog** — table of events, producers, observers, and transactional boundaries (`PostPublishedEvent`, `CustomPageChangedEvent`, `PostGitSyncRequestedEvent`, …).
- [ ] **Component vs form routes** — naming rule `/forms/*` (mutations) vs `/components/*` (fragments); document in Application-Guidelines §11.

### Medium priority (inconsistent or implicit)

- [ ] **`@Blocking`** — removed from codebase; delete any remaining references in old notes/rules (was for RESTEasy Reactive + JPA; verify if still needed after Quarkus upgrades).
- [ ] **`Given` builders** — add `Given.tag()`, `Given.serie()`, `Given.blogAudience()` (or document that tests must use `Given.transaction` + repositories).
- [ ] **Test isolation policy** — when to call `Given.cleanup()` vs `@Transactional` rollback vs dedicated test users (currently ad hoc).
- [ ] **OpenAPI** — convention for `@Operation(hidden = true)` on internal/HTMX endpoints vs public API docs.
- [x] **Toast helper** — documented in [HTMX-Events.md](HTMX-Events.md) §3–4 (`Toast` vs raw headers).
- [x] **HTMX scoped events** — [HTMX-Events.md](HTMX-Events.md): auth allowlist, lifecycle hooks, anti-patterns.
- [ ] **Deferred actions after login** — pattern for session-stored intents (follow-after-login); generalize or document as one-off.
- [ ] **Email** — document `quarkus.mailer.*`, mock in dev/test, `tb_email_notification_log` dedup rules.
- [ ] **Admin path naming** — `/review`, `/users`, `/blogs`, `/pages` are not under `/admin`; decide if rename or document as intentional.

### Low priority / nice to have

- [ ] **Error response shapes** — standard HTMX error swap HTML vs JSON for `/api/*`.
- [ ] **Pagination** — shared pattern (`shared/pagination`) usage in new list endpoints.
- [ ] **Slug rules** — central list: post, blog, tag (`TagSlug`), custom page (`CustomPagePaths`), reserved segments.
- [ ] **i18n** — not implemented; note if planned.
- [ ] **Security headers / CSRF** — document current approach (session cookie, HTMX same-origin).
- [ ] **Performance** — guidelines for `JOIN FETCH`, cache (`CustomPageCache`), feed query limits.
- [ ] **Jacoco / coverage** — no documented threshold or exclusions.

## Cursor / agent setup

| Item | Status |
|------|--------|
| `.cursor/rules/*.mdc` split by concern | ✅ |
| Root `AGENTS.md` index | ✅ |
| Legacy `.cursorrules` removed | ✅ (use `.cursor/rules/`) |
| File-scoped rules for tests | ✅ `contraponto-tests.mdc` |

## Implemented features (reference — keep ARCHITECTURE in sync)

- Multi-blog per user, `BlogAccess`
- `PostPublication` versioning + `PostChangeDiffService`
- Tags (`/tags/{slug}`) and series (`/serie/{slug}`)
- Blog follow + email subscribe + in-app notifications
- RSS: `/feed`, per-blog, per-serie, per-tag
- Git/Jekyll per-blog sync + scheduler
- User admin (`USER_ADMINISTRATOR`, `/users`)
- Custom pages: global `/page/{slug}`, user/blog scoped URLs via filter cache

## Suggested next doc PR

1. Refresh **Application-Guidelines.md** sections 2, 4, 9, 13 (routes + custom pages + admin).
2. Add **notification** and **publication** sections to Application-Guidelines.
3. Tick items in this checklist as each section lands.
