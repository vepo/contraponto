# Architecture & Conventions

Canonical reference for developers and AI agents. For route-level UX detail see [docs/application-guidelines.md](docs/application-guidelines.md). For REST segment taxonomy and the full path catalog see [docs/rest-url-guide.md](docs/rest-url-guide.md). For visual design see [docs/ui-guidelines.md](docs/ui-guidelines.md).

**Development process:** five-phase gate (feature analysis → architecture → tasks → approval → TDD) in [AGENTS.md](AGENTS.md) and `.cursor/rules/development-process.mdc`. Feature specs live in [feature/](feature/); transversal decisions in [docs/adr/](docs/adr/).

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

**Scoped refresh:** Form mutations return the smallest HTML (target fragment + optional `hx-swap-oob`), not a full page. Auth login/signup/logout OOB-update `#menu-container` and broadcast `loggedIn` / `loggedOut` on `body` so only declared chrome subscribers refetch themselves; `#main` stays unchanged. See [docs/htmx-events.md](docs/htmx-events.md).

### Internal route prefixes (`/__{feature}__/`)

Some public URLs would collide with `/{username}/…` blog routes or need a dedicated JAX-RS tree. **`@PreMatching` filters** rewrite them to an internal grouping prefix before routing:

| Pattern | Example | Filter | Internal endpoint |
|---------|---------|--------|-------------------|
| `/__{feature_snake_case}__` | `/__custom_page__` | `CustomPageFilter` | `CustomPageEndpoint` |
| `/__{feature_snake_case}__` | `/__activity_pub__` | `ActivityPubIngressFilter` | ActivityPub `*Endpoint` classes |

- **Builder:** `InternalRoutePrefixes.of("feature_snake_case")` in `shared.infra` — constants `CUSTOM_PAGE`, `ACTIVITY_PUB`.
- **Public URLs unchanged** — only the servlet path seen by JAX-RS is rewritten (same idea as custom pages).
- **Reserved segments:** internal prefix tokens (`__custom_page__`, `__activity_pub__`, …) are listed in `CustomPagePaths` so they cannot be usernames or blog slugs.
- **Never** link internal prefixes in HTML, sitemap, or RSS; list them in `CrawlerPrivatePaths`.
- **New ingress:** add a constant to `InternalRoutePrefixes`, a `*Paths` matcher, a `@PreMatching` filter, and register handlers only under the internal prefix — do not add protocol logic to `BlogEndpoint` / `PostEndpoint`.

See [docs/rest-url-guide.md](docs/rest-url-guide.md) §2 and §11.

## 3. Multi-blog model

- Each **user** has one **main** blog (auto-created) and optional secondary blogs (`tb_blogs`, unique `(owner_id, slug)`).
- **Posts** belong to a blog (`Post.blog`), not directly to a user.
- **Write** associates new posts with a selected blog (main or secondary).
- Only **blog owners** edit blog settings; **editors** and **administrators** may deactivate others' secondary blogs (`BlogAccess`).

### Public URL patterns

Each blog and post has a **dual blog URL**: platform path (below) and author **blog subdomain** (`https://{username}.{base-domain}/post/{slug}` for main blogs; secondary blogs add `/{blogSlug}`). Canonical links in SEO, sitemap, RSS, and email use the subdomain form when `app.blog-subdomain.enabled=true`.

| Resource | Main blog (platform path) | Secondary blog (platform path) | Main blog (subdomain) | Secondary blog (subdomain) |
|----------|---------------------------|--------------------------------|------------------------|------------------------------|
| Blog home | `GET /{username}` | `GET /{username}/{blogSlug}` | `GET /` on `{username}.{base}` | `GET /{blogSlug}` |
| Post | `GET /{username}/post/{slug}` | `GET /{username}/{blogSlug}/post/{slug}` | `GET /post/{slug}` | `GET /{blogSlug}/post/{slug}` |
| Serie | `GET /{username}/serie/{serieSlug}` | `GET /{username}/{blogSlug}/serie/{serieSlug}` | `GET /serie/{serieSlug}` | `GET /{blogSlug}/serie/{serieSlug}` |
| Custom page | `GET /{username}/page/{slug}` | `GET /{username}/{blogSlug}/page/{slug}` | `GET /page/{slug}` | `GET /{blogSlug}/page/{slug}` |
| Global custom page | `GET /page/{slug}` | — | — (platform host only) | — |

Use `BlogPublicUrlService` in templates, SEO, RSS, and emails for reader-facing links. Internal path builders `PostPaths.extractUrl(post)` and `BlogPaths.extractUrl(blog)` remain the platform-path form used after `BlogSubdomainFilter` rewrites subdomain requests.

`CustomPageFilter` rewrites public custom-page URLs to `/__custom_page__/...`; `CustomPageEndpoint` loads published pages via `CustomPageCache` (in-memory, invalidated on `CustomPageChangedEvent`).

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
- **Follow after login** — guest opens login modal from Follow; after login, client clicks Follow again (no server-side pending intent).

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

## 8.1 ActivityPub federation (planned)

Server-to-server [ActivityPub](https://www.w3.org/TR/activitypub/) so authors syndicate **main-blog** posts to Mastodon and other Fediverse apps. Each opted-in author is a **Person** actor on their **blog subdomain**; **WebFinger** resolves `@user@domain`; **inbox** receives Follow/Undo; **outbox** lists Create activities; delivery uses **HTTP Signatures** and an async retry queue on `PostPublishedEvent` / `PostUnpublishedEvent`.

| Resource | Path (planned) | Notes |
|----------|----------------|-------|
| WebFinger | `GET /.well-known/webfinger?resource=acct:user@domain` | Actor discovery |
| Actor | `GET /` (subdomain) or actor document path per ADR-0008 | `application/activity+json` |
| Inbox | `POST /{username}/inbox` | Follow, Accept, Create delivery target |
| Outbox | `GET /{username}/outbox` | OrderedCollection of activities |

**Docs:** [feature/activitypub-integration.md](feature/activitypub-integration.md), ADRs [0006](docs/adr/0006-activitypub-federation.md)–[0008](docs/adr/0008-activitypub-actor-identity.md) (**Accepted** 2026-07-06).

## 8.2 SEO

| Resource | Path | Notes |
|----------|------|--------|
| Page metadata | Per-route `SeoMetadata` in Qute `components/seo-meta-tags.html` | Title, description, canonical, Open Graph, Twitter Card, `article:published_time` / `article:modified_time`, JSON-LD (`BlogPosting`, `BreadcrumbList`, `WebSite` + `SearchAction`, etc.) |
| SEO fragment (HTMX) | `GET /components/seo?path=...` | Refreshes `#seo-head` after navigation or history restore |
| Author directory | `GET /authors` | Public list of authors with published posts |
| Blog directory | `GET /explore/blogs` | Public list of active blogs |
| Sitemap | `GET /sitemap.xml` | Published URLs with `lastmod`, post cover `image:image` entries; cached (`sitemap` cache, invalidated on publish / custom page change) |
| Robots | `GET /robots.txt` | Shared `CrawlerPrivatePaths` disallow list (manage/auth/components/search/feed, …); `Sitemap:` uses `image.base.url` / `APP_PUBLIC_URL` |
| Post slug alias | `tb_post_slug_aliases` | Old slugs registered on republish; `GET` post routes 301 to current URL |
| Related posts | Post page block | Tag-overlap related articles with crawler-friendly `href` links |
| Favicon | `GET /favicon.svg` | Linked from `components/head.html` |
| RSS crawl hint | Feed responses | `X-Robots-Tag: noindex` via `RssNoIndexFilter` |

Set **`APP_PUBLIC_URL`** in production so canonical URLs, `og:image`, and the sitemap locs use the public origin. Submit `https://your-domain/sitemap.xml` in Google Search Console after deploy (see [docs/deployment.md](docs/deployment.md)). HTMX OOB + `main.js` keep head metadata in sync with `hx-push-url` navigation (see [docs/htmx-events.md](docs/htmx-events.md)).

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
| `PostPublishedEvent` | `PostPublishedNotificationObserver`; `RssFeedCacheInvalidator` (clears `rss-feeds` cache) |
| `PostGitSyncRequestedEvent` | Git export |
| `CustomPageChangedEvent` | `CustomPageCache` refresh |

### Testing

- **`@WebTest`** — browser tests with `App` DSL and `Given` builders.
- **`Given`**: `user()`, `post()`, `blog()`, `customPage()`, `cleanup()`, `randomCover()`.
- Self-contained tests; call `Given.cleanup()` when mutating shared data.

## 10. Package layout

Bounded contexts and allowed dependencies: [docs/domain-specification.md](docs/domain-specification.md) §Bounded contexts. CDI event catalog: [docs/cdi-events.md](docs/cdi-events.md).

```
dev.vepo.contraponto/
├── admin/            # Author workspace — editor review (featured posts) — /review
├── auth/             # Identity & access — passwords, account email, tokens
├── blog/             # Content publishing — blog CRUD, public blog pages
├── comment/          # Reader engagement — post comments
├── components/       # Presentation shell — header, menu, forms (login, publish, draft)
├── content/          # Content publishing — render plugins, post body rendering
├── custompage/       # Customization — static pages, filter, cache
├── dashboard/        # Author workspace — manage analytics
├── directory/        # Discovery — author/blog directories
├── git/              # Integration — Jekyll/Git sync
├── activitypub/      # Integration — ActivityPub federation
├── highlight/        # Reader engagement — text highlights, proposals, notes
├── home/             # Discovery — featured homepage
├── image/            # Media — upload & storage
├── library/          # Author workspace — drafts/published library
├── navigation/       # Presentation shell — menu hubs, breadcrumbs
├── notification/     # Reader engagement — follow, subscribe, in-app notifications
├── platforminsights/ # Platform insights — admin visitor analytics
├── post/             # Content publishing — posts, publications, public post view
├── postresponse/     # Reader engagement — author responses to highlights
├── profile/          # Author workspace — author profile edit
├── readinglist/      # Reader engagement — saved posts, mark as read
├── readingtime/      # Reader engagement — reading progress
├── renderer/         # Content publishing — Markdown / AsciiDoc
├── rss/              # Discovery — RSS endpoints
├── search/           # Discovery — site search
├── seo/              # Discovery — sitemap, robots, metadata
├── serie/            # Content publishing — series pages
├── shared/           # Shared kernel — infra, i18n, pagination, security (test: App, Given)
├── tag/              # Content publishing — tag pages & editor tools
├── user/             # Identity & access — users, roles, session, /users admin
├── view/             # Discovery — view counts
└── write/            # Content publishing — write editor entry
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

## 12. Navigation hubs and breadcrumbs

| Hub | Path | Role |
|-----|------|------|
| Writing | `GET /writing` | `@Logged` |
| Manage | `GET /manage` | `@Logged` |
| Account | `GET /account` | `@Logged` |
| Review | `GET /editor` | `EDITOR` / `ADMIN` |
| Administration | `GET /administration` | `USER_ADMINISTRATOR` / `ADMIN` |

`BreadcrumbService` builds `BreadcrumbTrail` for full-page templates; render via `components/breadcrumb.html`. Public blog/post pages use **Home** as root; manage pages use the hub name.

## 13. HTMX conventions

- `data-hx-get` on links for progressive enhancement; `hx-post` on forms.
- Target `main` for full page swaps; `#modal-container` for modals.
- `X-Toast-Message` header (or toast helper) after mutations.
- `hx-push-url="true"` when the URL should change.
- Form validation errors: `hx-target-error` / `hx-swap-error` where used.

## 14. Authentication & roles

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

## 15. Database (main tables)

- `tb_users`, `tb_user_roles`
- `tb_blogs` (+ git columns)
- `tb_posts`, `tb_post_tags`, `tb_post_publications`, `tb_post_publication_tags`
- `tb_series`, `tb_tags`
- `tb_images`, `tb_views`
- `tb_custom_pages`
- `tb_blog_audience`, `tb_notifications`, `tb_email_notification_log`

Full DDL: `src/main/resources/db/migration/V0.0.1__initial_schema.sql`

## 16. Adding a feature (checklist)

1. Flyway migration if schema changes.
2. Entity + repository (+ service if non-trivial).
3. Endpoint + Qute template(s).
4. Access rules (`BlogAccess` / `UserAccess` / `@Logged`).
5. Fire CDI events if caches or async reactions exist.
6. `@WebTest` + `App` DSL updates.
7. Header/footer/navigation links if user-visible.
8. Update [docs/application-guidelines.md](docs/application-guidelines.md) for new routes.
9. Update [docs/feature-catalog.md](docs/feature-catalog.md) if user-facing navigation or routes changed (step counts and menu paths).

## 17. Testing example

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

## 18. Common pitfalls

- **Custom pages** — always fire `CustomPageChangedEvent` after save/delete; slugs stored with leading `/` (`CustomPagePaths.storedSlug`).
- **Post URLs** — use `PostPaths.extractUrl`, especially for secondary blogs.
- **HTMX tests** — wait for swaps (`App` helpers / `waitForReady`).
- **N+1 in templates** — `JOIN FETCH` in repository queries.
- **Git tests** — `%test.contraponto.git.poll-enabled=false` (scheduler off in tests).
- **Own blog** — cannot follow/subscribe to your own blog (`BlogAudienceService`).

## 19. Caching and session storage

### Deployment model

| Deployment | Auth sessions (`__session`) | Custom pages | RSS XML |
|------------|----------------------------|--------------|---------|
| **Single instance** (default dev/small prod) | `app.session.store=memory` — JVM map in `InMemorySessionStore` | `CustomPageCache` per process | Caffeine via `quarkus-cache` (`rss-feeds`) |
| **Multiple instances** | `app.session.store=redis` + `quarkus.redis.hosts` — shared `RedisSessionStore` | Same (per-instance cache; invalidate on all nodes via DB read on miss, or accept brief staleness) | Shared invalidation still per-node Caffeine; TTL 5m bounds staleness |

Login sessions store **user id only** (`SessionStore`); `LoggedUserProvider` reloads `User` from PostgreSQL on each request so role/profile changes apply without stale entities.

Anonymous **view** / **reading** sessions remain in PostgreSQL (`__view_session` cookie → `tb_views`, `tb_reading_sessions`) — not Redis.

### Configuration (selected)

```properties
quarkus.datasource.db-kind=postgresql
quarkus.flyway.migrate-at-start=true
%dev.quarkus.flyway.clean-at-start=true
image.base.url=${APP_PUBLIC_URL:http://localhost:8080}
app.session.store=memory
app.session.ttl-seconds=2592000
# Multi-instance production example:
# app.session.store=redis
# quarkus.redis.hosts=redis://redis:6379
quarkus.cache.caffeine."rss-feeds".expire-after-write=5M
contraponto.git.poll-enabled=true
%prod.contraponto.git.poll-enabled=false
%prod.app.secure-cookies=true
%dev.quarkus.mailer.mock=true
app.show-error-details=false
%dev.app.show-error-details=true
app.dev-import.enabled=false
%dev.app.dev-import.enabled=true
```

See `application.properties`, [docs/deployment.md](docs/deployment.md), and [docs/git-jekyll-convention.md](docs/git-jekyll-convention.md).

## 20. Jakarta EE vs Quarkus-specific APIs

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

## 20. Interface internationalization (i18n)

- **Default locale:** `pt-BR` — canonical copy in Qute templates (`data-i18n` keys + Portuguese text).
- **Secondary locales:** `en`, `es` — JSON bundles at `GET /i18n/messages/{locale}.json` (`src/main/resources/i18n/`).
- **Client:** `js/i18n.js` loads bundles when locale ≠ `pt-BR`, applies translations on load and after HTMX `afterSettle`.
- **Preference:** cookie `contraponto_locale`; `POST /forms/locale` to switch; header/footer **language switcher** + Account hub hint.
- **Scope:** menus, forms, validation chrome, toasts — not post/comment/blog body, custom page DB content, or account emails.
- **Server toasts:** `Toast.i18nKey(key, ptBrDefault)` + `I18nDefaults` / `I18nKeys`; client resolves via `toast.js`.
