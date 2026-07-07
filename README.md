# Contraponto — A Blog Platform

Multi-author publishing with HTMX-driven navigation, per-user blogs, versioning, tags, series, notifications, and optional Git/Jekyll sync.

## Tech stack

| Layer | Technology |
|-------|------------|
| Backend | Quarkus (Java 17+) |
| Templates | Qute |
| UI dynamics | HTMX |
| Database | PostgreSQL, Hibernate ORM, Flyway |
| Tests | JUnit 5, Selenium, `App` DSL + `Given` builders |
| Build | Maven |

## Quick start

```bash
./mvnw quarkus:dev
```

Open [http://localhost:8080](http://localhost:8080). Dev mode runs Flyway clean+migrate (`%dev.quarkus.flyway.clean-at-start=true`) and loads sample data from `dev-import.sql`.

Default admin (from migration): `admin` — rotate password after first deploy (see [docs/deployment.md](docs/deployment.md)).

## Documentation

| Doc | Contents |
|-----|----------|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Patterns, URL map, schema overview, how to add features |
| [AGENTS.md](AGENTS.md) | Index for AI / Cursor agents |
| [docs/conventions-checklist.md](docs/conventions-checklist.md) | Doc gaps and conventions still to formalize |
| [docs/application-guidelines.md](docs/application-guidelines.md) | Route and flow descriptions |
| [docs/ui-guidelines.md](docs/ui-guidelines.md) | Typography, colors, components |
| [docs/git-jekyll-convention.md](docs/git-jekyll-convention.md) | Git repository layout for sync |
| [docs/greenfield-deployment-tutorial.md](docs/greenfield-deployment-tutorial.md) | First production deploy (server, DNS, TLS, Docker) |
| [docs/deployment.md](docs/deployment.md) | Production env-var checklist |
| [.cursor/rules/](.cursor/rules/) | Cursor project rules |

## Project structure

```
src/main/java/dev/vepo/contraponto/
├── admin/          # Editor review (/review)
├── auth/           # Password service
├── blog/           # Blogs (multi per user), public blog pages
├── components/     # Header, menu, auth/publish forms
├── custompage/     # Static pages + cache filter
├── dashboard/      # Author dashboard
├── git/            # Git ↔ Jekyll import/export
├── home/           # Featured homepage
├── image/          # Image upload API
├── library/        # Drafts & published lists
├── notification/   # Follow, subscribe, in-app + email
├── post/           # Posts, publication snapshots
├── profile/        # Profile settings
├── renderer/       # Markdown / AsciiDoc
├── rss/            # RSS feeds
├── search/         # Modal + full-page search
├── serie/          # Post series
├── shared/         # LoggedUser, pagination, test helpers
├── tag/            # Tag pages & curation
├── user/           # Users & admin (/users)
├── view/           # View counts
└── write/          # Editor
```

## Features

- **Multi-blog** — main + secondary blogs per user; public URLs by username and blog slug
- **Draft / publish** — working copy on `Post`; immutable `PostPublication` per publish; version history
- **Tags & series** — taxonomy, tag landing pages, serialized posts
- **Custom pages** — global, per-user main blog, or per secondary blog (`/page/...` URLs)
- **Audience** — follow (in-app notifications) and email subscribe per blog
- **Notifications** — in-app inbox, badge, mark-as-read
- **Featured posts** — editors curate the homepage (`/review`)
- **Search** — HTMX modal + paginated `/search`
- **RSS** — site, blog, serie, and tag feeds
- **Git sync** — optional Jekyll-shaped repos per blog ([convention](docs/git-jekyll-convention.md))
- **Fediverse (ActivityPub)** — authors opt in to syndicate main-blog posts to Mastodon and compatible servers ([feature doc](feature/activitypub-integration.md))
- **Roles** — `USER`, `EDITOR`, `USER_ADMINISTRATOR`, `ADMIN`
- **HTMX** — SPA-like navigation, toasts, modals

## Useful routes

| Area | Path |
|------|------|
| Home | `/` |
| Write | `/write`, `/write/draft/{id}` |
| Writing hub | `/writing`, `/writing/library`, `/writing/blogs`, `/writing/appearance` |
| Manage hub | `/manage`, `/manage/dashboard`, `/manage/pages`, `/manage/comments` |
| Account hub | `/account`, `/account/security`, `/account/notifications`, `/account/subscriptions` |
| Editor hub | `/editor`, `/editor/review`, `/editor/tags` |
| Administration | `/administration/users` |
| Search | `/search` |
| Blog edit | `/blogs/{id}/edit`, `/blogs/new` |
| Tags (public) | `/tags/{slug}` |
| Site RSS | `/feed` |

Post and blog URLs: `/{username}/post/{slug}`, `/{username}/{blogSlug}/post/{slug}`, etc. (see ARCHITECTURE.md).

## Testing

```bash
./mvnw test
```

- `@WebTest` — Quarkus + Selenium; inject `App` for navigation.
- `Given.user()`, `Given.post()`, `Given.blog()`, `Given.customPage()` for data setup.
- `Given.cleanup()` when tests leave shared data behind.

## Cursor / AI setup

Rules live in `.cursor/rules/` (`contraponto-core.mdc` always applies). Start from [AGENTS.md](AGENTS.md).
