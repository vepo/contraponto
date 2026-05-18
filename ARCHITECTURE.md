# Architecture & Conventions

Canonical reference for developers and AI agents. For route-level UX detail see [docs/Application-Guidelines.md](docs/Application-Guidelines.md). For visual design see [docs/UI-Guidelines.md](docs/UI-Guidelines.md).

## 1. Core principles

- **No client-side framework** — HTMX loads HTML fragments from the server.
- **SPA-like navigation** — Links use `data-hx-get` (or `hx-get`) with `hx-select="main" hx-target="main" hx-push-url="true"`.
- **REST/JSON** — Image upload (`/api/images`) and a few JSON APIs; everything else is HTML.
- **Qute templates** — `.html` files beside the endpoint class; `@CheckedTemplate` native methods.
- **Schema migrations** — Flyway scripts in `src/main/resources/db/migration/` (`quarkus.flyway.*`).

## 2. Request lifecycle

1. Browser request (often HTMX).
2. JAX-RS endpoint (`@ApplicationScoped`, `@Path`).
3. Repository and/or `@ApplicationScoped` service.
4. Qute `TemplateInstance` or `Response` (with optional `X-Toast-Message`).
5. HTMX swaps `#main` or a partial target.

**Scoped refresh:** Form mutations return the smallest HTML (target fragment + optional `hx-swap-oob`), not a full page. Auth login/signup/logout OOB-update `#menu-container` and broadcast `loggedIn` / `loggedOut` on `body` so only declared chrome subscribers refetch themselves; `#main` stays unchanged. See [docs/HTMX-Events.md](docs/HTMX-Events.md).

## 3. Multi-blog model

- Each **user** has one **main** blog (auto-created) and optional secondary blogs (`tb_blogs`, unique `(owner_id, slug)`).
- **Posts** belong to a blog (`Post.blog`), not directly to a user.
- **Write** associates new posts with a selected blog (main or secondary).
- **Editors** can manage any blog (`BlogAccess.canEdit`); owners manage their own.

### Public URL patterns

| Resource | Main blog | Secondary blog |
|----------|-----------|----------------|
| Blog home | `GET /{username}` | `GET /{username}/{blogSlug}` |
| Post | `GET /{username}/post/{slug}` | `GET /{username}/{blogSlug}/post/{slug}` |
| Serie | `GET /{username}/serie/{serieSlug}` | `GET /{username}/{blogSlug}/serie/{serieSlug}` |
| Custom page | `GET /{username}/page/{slug}` | `GET /{username}/{blogSlug}/page/{slug}` |
| Global custom page | `GET /page/{slug}` | — |

Use `PostEndpoint.extractUrl(post)` and `CustomPagePaths.publicUrl(page)` in code — do not duplicate path logic.

`CustomPageFilter` serves published pages from `CustomPageCache` (no DB hit). Internal preview routes live under `/_custom_page/...`.

## 4. Post publications (versioning)

- **`Post`** — working copy (draft or published flag, current tags/serie/cover).
- **`PostPublication`** — immutable snapshot per publish (`version`, content, tags at publish time).
- **`Post.livePublication`** — pointer to the snapshot shown to readers.
- **`PostPublicationService`** — creates snapshots, detects unpublished edits vs live snapshot, fires `PostPublishedEvent`.
- **`PostChangeDiffService`** — version history on the post page for authors.

Republishing increments version and triggers notifications again.

## 5. Tags & series

- **Tags** — global taxonomy (`tb_tags`); posts link via `tb_post_tags`; publication snapshots copy tags to `tb_post_publication_tags`.
- **Tag pages** — `GET /tags/{slug}` (+ RSS at `/tags/{slug}/feed`). Editors edit metadata at `/tags/{slug}/edit`.
- **Series** — per-blog ordered collections (`tb_series`); posts optional `serie_id`; public serie pages under `/serie/...`.

## 6. Notifications & audience

- **`BlogAudience`** — per user/blog: `followed` and/or `email_subscribed` (`tb_blog_audience`).
- **Follow** — `POST /forms/blogs/{blogId}/follow` (in-app notifications for new posts).
- **Subscribe** — `POST /forms/blogs/{blogId}/subscribe` (email on publish, deduped via `tb_email_notification_log`).
- **`PostPublishedNotificationObserver`** — on `PostPublishedEvent`, notifies followers and emails subscribers.
- **In-app** — `GET /notifications`, badge at `/components/notifications/badge`, mark read via `/forms/notifications/read`.
- **Deferred follow after login** — session stores pending blog id; `LoginEndpoint` completes follow on success.

## 7. Git / Jekyll sync

- Per-blog flags: `git_enabled`, `git_remote_url`, `git_branch`, `git_last_known_commit`.
- **Export/import** — Jekyll-style `_posts/`, `_drafts/`, YAML front matter (`docs/git-jekyll-convention.md`).
- **Scheduler** — `GitRemotePollScheduler` when `contraponto.git.poll-enabled=true`.
- **Events** — `PostGitSyncRequestedEvent` after publish; `GitPostCommittedObserver` for outbound commits.

## 8. RSS feeds

| Feed | Path |
|------|------|
| Site-wide | `GET /feed` |
| User main blog | `GET /{username}/feed/main-blog` |
| User secondary blog | `GET /{username}/{blogSlug}/feed` |
| Serie | `.../serie/{serieSlug}/feed` |
| Tag | `GET /tags/{slug}/feed` |

Registered before catch-all `/{username}` routes where needed (`SiteWideFeedEndpoint`).

## 9. Design patterns

### Repository

- One per entity; `EntityManager`; `@Transactional` on writes.

### Service layer

Use when logic spans entities or fires events: `PostPublicationService`, `TagService`, `SerieService`, `BlogAudienceService`, `NotificationService`, `BlogGitIntegrationService`, etc.

### Access helpers

- `BlogAccess` — create/edit/delete/list blogs.
- `UserAccess` — role assignment rules for user admin UI.
- `CustomPageAccess` — who can manage pages.

### CDI events

| Event | Typical consumer |
|-------|------------------|
| `PostPublishedEvent` | `PostPublishedNotificationObserver` |
| `PostGitSyncRequestedEvent` | Git export |
| `CustomPageChangedEvent` | `CustomPageCache` refresh |

### Testing

- **`@WebTest`** — browser tests with `App` DSL and `Given` builders.
- **`Given`**: `user()`, `post()`, `blog()`, `customPage()`, `cleanup()`, `randomCover()`.
- Self-contained tests; call `Given.cleanup()` when mutating shared data.

## 10. Package layout

```
dev.vepo.contraponto/
├── admin/          # Editor review (featured posts) — /review
├── auth/           # Password hashing
├── blog/           # Blog CRUD, public blog pages
├── components/     # Header, menu, forms (login, publish, draft)
├── custompage/     # Static pages, filter, cache
├── dashboard/      library/  profile/  search/  write/
├── git/            # Jekyll/Git sync
├── home/           # Featured homepage
├── image/          # Upload & storage
├── notification/   # Follow, subscribe, in-app notifications
├── post/           # Posts, publications, public post view
├── renderer/       # Markdown / AsciiDoc
├── rss/            # RSS endpoints
├── serie/          # Series pages
├── shared/         # infra (LoggedUser, TemplateExtensions), test (App, Given)
├── tag/            # Tag pages & editor tools
├── user/           # Users, roles, /users admin
└── view/           # View counts
```

## 11. Naming

| Kind | Pattern |
|------|---------|
| Endpoint | `XxxEndpoint` |
| Repository | `XxxRepository` |
| Service | `XxxService` |
| Entity | singular PascalCase |
| Test (browser) | `XxxTest` with `@WebTest` |
| Template file | `kebab-case.html` |

## 12. HTMX conventions

- `data-hx-get` on links for progressive enhancement; `hx-post` on forms.
- Target `main` for full page swaps; `#modal-container` for modals.
- `X-Toast-Message` header (or toast helper) after mutations.
- `hx-push-url="true"` when the URL should change.
- Form validation errors: `hx-target-error` / `hx-swap-error` where used.

## 13. Authentication & roles

- Session cookie `__session`; `LoggedUser` request-scoped bean.
- `@Logged` interceptor — redirect or deny when not authenticated.
- **Roles**: `USER`, `EDITOR`, `USER_ADMINISTRATOR`, `ADMIN` (stored in `tb_user_roles`, a user may have several).

| Capability | Roles / rule |
|------------|----------------|
| Write own content | Authenticated owner |
| Feature posts (homepage) | `EDITOR` |
| Review page `/review` | `EDITOR` |
| Manage users `/users` | `USER_ADMINISTRATOR` or `ADMIN` |
| Assign `ADMIN` role | `ADMIN` only |

## 14. Database (main tables)

- `tb_users`, `tb_user_roles`
- `tb_blogs` (+ git columns)
- `tb_posts`, `tb_post_tags`, `tb_post_publications`, `tb_post_publication_tags`
- `tb_series`, `tb_tags`
- `tb_images`, `tb_views`
- `tb_custom_pages`
- `tb_blog_audience`, `tb_notifications`, `tb_email_notification_log`

Full DDL: `src/main/resources/db/migration/V0.0.1__Initial_schema.sql`

## 15. Adding a feature (checklist)

1. Flyway migration if schema changes.
2. Entity + repository (+ service if non-trivial).
3. Endpoint + Qute template(s).
4. Access rules (`BlogAccess` / `UserAccess` / `@Logged`).
5. Fire CDI events if caches or async reactions exist.
6. `@WebTest` + `App` DSL updates.
7. Header/footer/navigation links if user-visible.
8. Update [docs/Application-Guidelines.md](docs/Application-Guidelines.md) for new routes.

## 16. Testing example

```java
@WebTest
class ExampleTest {
    @Test
    void testLogin(App app) {
        app.access()
           .loginModal()
           .useLogin("user@example.com")
           .usePassword("pass")
           .submit()
           .assertModalWasClosed()
           .assertMenuIsDisplayed();
    }
}
```

## 17. Common pitfalls

- **Custom pages** — always fire `CustomPageChangedEvent` after save/delete; slugs stored with leading `/` (`CustomPagePaths.storedSlug`).
- **Post URLs** — use `PostEndpoint.extractUrl`, especially for secondary blogs.
- **HTMX tests** — wait for swaps (`App` helpers / `waitForReady`).
- **N+1 in templates** — `JOIN FETCH` in repository queries.
- **Git tests** — `%test.contraponto.git.poll-enabled=false` (scheduler off in tests).
- **Own blog** — cannot follow/subscribe to your own blog (`BlogAudienceService`).

## 18. Configuration (selected)

```properties
quarkus.datasource.db-kind=postgresql
quarkus.flyway.migrate-at-start=true
%dev.quarkus.flyway.clean-at-start=true
image.storage.path=/tmp/contraponto-images
contraponto.git.poll-enabled=true
contraponto.git.poll-interval=2m
%dev.quarkus.mailer.mock=true
app.show-error-details=false
%dev.app.show-error-details=true
app.dev-import.enabled=false
%dev.app.dev-import.enabled=true
```

See `application.properties` and [docs/git-jekyll-convention.md](docs/git-jekyll-convention.md).

## 19. Jakarta EE vs Quarkus-specific APIs

**Prefer Jakarta / MicroProfile in application code** where a portable API exists:

| Concern | Portable approach | Used in |
|---------|-------------------|---------|
| JTA `REQUIRES_NEW` (async git) | `@Transactional(TxType.REQUIRES_NEW)` on `BlogGitIntegrationTransaction` | `git/` |
| Test transactions | `TestTransactionRunner` + `jakarta.transaction.Transactional` | `Given.transaction` |
| CDI lookup from Qute extensions | `CDI.current().select(...)` + injected `RenderedHtmlEnricher` | `TemplateExtensions` |
| Dev error details | `app.show-error-details` via `@ConfigProperty` | `GenericExceptionMapper` |
| Dev SQL seed | `app.dev-import.enabled` + `@Initialized(ApplicationScoped.class)` | `DatabaseDevSetup` |

**Intentionally Quarkus-bound (no Jakarta EE equivalent)** — do not replace without a stack change:

| API | Role |
|-----|------|
| **Qute** (`io.quarkus.qute.*`, `@CheckedTemplate`) | Server-rendered HTML for all endpoints |
| **Quarkus Scheduler** (`@Scheduled`) | `GitRemotePollScheduler` |
| **Quarkus Mailer** (`Mailer`, `Mail`) | `PostNotificationEmailService` |
| **`@RegisterForReflection`** | Native-image metadata on `Page`, `SubscriptionRow`, `BlogAudienceView` |
| **Quarkus test** (`@QuarkusTest`, `TestHTTPResource`, `MockMailbox`) | Integration and browser tests |
| **SmallRye `@ConfigMapping`** | `ContrapontoGitConfig` (MicroProfile Config ecosystem) |

REST, JPA, CDI, Bean Validation, and JTA elsewhere already use `jakarta.*` types.
