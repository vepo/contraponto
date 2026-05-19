# Conventions checklist

Tracks what is **documented and enforced** vs **implemented but under-documented** vs **missing as a team convention**. Update this when closing gaps.

## Doc file naming (`docs/`)

Use **kebab-case** (lowercase words separated by hyphens): `application-guidelines.md`, `htmx-events.md`. Acronyms are lowercase in the filename (`htmx`, not `HTMX`). Root-level repo docs (e.g. `ARCHITECTURE.md`) keep their existing names.

Legend: ✅ done · ⚠️ partial · ❌ missing / outdated

## Documentation map

| Area | ARCHITECTURE | application-guidelines | ui-guidelines | Cursor rules |
|------|:------------:|:----------------------:|:-------------:|:------------:|
| Multi-blog URLs | ✅ | ⚠️ | — | ✅ |
| Post publications / versions | ✅ | ❌ | — | ✅ |
| Tags & series | ✅ | ❌ | ⚠️ | ⚠️ |
| Notifications & audience | ✅ | ❌ | ⚠️ | ✅ |
| UI element catalog (BEM, CSS bundles) | — | — | — | ✅ `ui-elements.md` + `contraponto-ui.mdc` + `contraponto-css.mdc` |
| Git/Jekyll sync | ✅ (link) | ❌ | — | ✅ |
| RSS feeds | ✅ | ❌ | — | — |
| Custom page URL shapes | ✅ | ⚠️ (old `/page/{username}/...`) | — | ✅ |
| Roles (`USER_ADMINISTRATOR`) | ✅ | ❌ | — | ✅ |
| User/blog admin UI | ✅ | ❌ | — | — |
| UI feature catalog (paths & step counts) | ✅ | — | — | ✅ [feature-catalog.md](feature-catalog.md) + `feature-catalog.mdc` |
| HTMX events / scoped refresh | ✅ | — | — | ✅ [htmx-events.md](htmx-events.md) |
| JavaScript modules / HTMX-first | ✅ | — | — | ✅ `contraponto-javascript.mdc` |
| CSS authoring (tokens, bundles) | — | — | ✅ [ui-elements.md](ui-elements.md) | ✅ `contraponto-css.mdc` |

## Conventions to adopt or document

### High priority (code exists, docs lag)

- [ ] **Update [application-guidelines.md](application-guidelines.md)** — notifications, tags, series, publications, `/users`, `/blogs`, `/pages`, RSS, git, corrected custom-page URLs (`CustomPagePaths`). UI access paths: see [feature-catalog.md](feature-catalog.md).
- [x] **Update [ui-guidelines.md](ui-guidelines.md)** — notification bell/inbox, follow/subscribe, tag/serie, comments, subscriptions, version history (see §18–23).
- [x] **UI element catalog** — [ui-elements.md](ui-elements.md); Cursor rule `contraponto-ui.mdc`; CSS split main / manage / write.
- [ ] **Service layer guideline** — when logic belongs in `XxxService` vs endpoint vs repository (ARCHITECTURE §5 is a start; add examples per package).
- [ ] **CDI events catalog** — table of events, producers, observers, and transactional boundaries (`PostPublishedEvent`, `CustomPageChangedEvent`, `PostGitSyncRequestedEvent`, …).
- [ ] **Component vs form routes** — naming rule `/forms/*` (mutations) vs `/components/*` (fragments); document in application-guidelines §11.

### Medium priority (inconsistent or implicit)

- [ ] **`@Blocking`** — removed from codebase; delete any remaining references in old notes/rules (was for RESTEasy Reactive + JPA; verify if still needed after Quarkus upgrades).
- [ ] **`Given` builders** — add `Given.tag()`, `Given.serie()`, `Given.blogAudience()` (or document that tests must use `Given.transaction` + repositories).
- [ ] **Test isolation policy** — when to call `Given.cleanup()` vs `@Transactional` rollback vs dedicated test users (currently ad hoc).
- [ ] **OpenAPI** — convention for `@Operation(hidden = true)` on internal/HTMX endpoints vs public API docs.
- [x] **Toast helper** — documented in [htmx-events.md](htmx-events.md) §3–4 (`Toast` vs raw headers).
- [x] **HTMX scoped events** — [htmx-events.md](htmx-events.md): auth allowlist, lifecycle hooks, anti-patterns.
- [ ] **Deferred actions after login** — pattern for session-stored intents (follow-after-login); generalize or document as one-off.
- [x] **Email** — `quarkus.mailer.*`, mock in dev/test; post-publish dedup via `tb_email_notification_log`; account emails in domain spec §UI labels and `auth/AccountEmailService`.
- [ ] **Admin path naming** — `/review`, `/users`, `/blogs`, `/pages` are not under `/admin`; decide if rename or document as intentional.
- [ ] **Inline `onclick` in templates** — migrate locale picker, modal close, error retry to HTMX or `js/*.js` managers (do not add new inline handlers).
- [ ] **Hard-coded colors in `write.css`** — align with `:root` tokens when editing write surfaces.

### Low priority / nice to have

- [ ] **Error response shapes** — standard HTMX error swap HTML vs JSON for `/api/*`.
- [x] **Pagination** — reading = Load more; managing = numbered footer (`shared/pagination`, `components/load-more-posts.html`, `components/manage-pagination.html`). See `.cursor/rules/contraponto-pagination.mdc`.
- [ ] **Slug rules** — central list: post, blog, tag (`TagSlug`), custom page (`CustomPagePaths`), reserved segments.
- [x] **i18n** — client-side `data-i18n`; default `pt-BR` in templates; `en`/`es` JSON at `/i18n/messages/{locale}.json`.
- [ ] **Security headers / CSRF** — document current approach (session cookie, HTMX same-origin).
- [x] **Performance** — `CustomPageCache`, `RssFeedService` (`rss-feeds` cache); see ARCHITECTURE §19.
- [ ] **Jacoco / coverage** — no documented threshold or exclusions.

## Cursor / agent setup

| Item | Status |
|------|--------|
| `.cursor/rules/*.mdc` split by concern | ✅ |
| Root `AGENTS.md` index | ✅ |
| Legacy `.cursorrules` removed | ✅ (use `.cursor/rules/`) |
| File-scoped rules for tests | ✅ `contraponto-tests.mdc` |
| Feature catalog maintenance rule | ✅ `feature-catalog.mdc` (always-on) |
| JavaScript / HTMX-first rule | ✅ `contraponto-javascript.mdc` (globs: `js/*.js`, templates) |
| CSS authoring rule | ✅ `contraponto-css.mdc` (globs: `style/*.css`, templates) |

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

1. Refresh **application-guidelines.md** sections 2, 4, 9, 13 (routes + custom pages + admin).
2. Add **notification** and **publication** sections to application-guidelines.
3. Tick items in this checklist as each section lands.
