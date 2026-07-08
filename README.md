# Contraponto

**Self-hostable, multi-author blogging platform** — server-rendered with [Quarkus](https://quarkus.io/), [Qute](https://quarkus.io/guides/qute), and [HTMX](https://htmx.org/). No React, Vue, or SPA build step.

[![CI](https://github.com/vepo/contraponto/actions/workflows/ci.yml/badge.svg)](https://github.com/vepo/contraponto/actions/workflows/ci.yml)
[![License: GPL v2](https://img.shields.io/badge/License-GPL%20v2-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17%2B-orange)](https://openjdk.org/)
[![Quarkus](https://img.shields.io/badge/Quarkus-framework-blue)](https://quarkus.io/)

Publish on your own domain, run several blogs per author, syndicate to the Fediverse, and keep a Git-backed archive — while readers get search, RSS, reading lists, highlights, and comments without a heavy client bundle.

**Live reference:** [blogs.commit-mestre.dev](https://blogs.commit-mestre.dev) (production instance of this codebase).

---

## Why Contraponto?

| | |
|---|---|
| **HTMX-first UX** | SPA-like navigation (`hx-push-url`, fragment swaps) with HTML from the server — predictable, crawlable, accessible. |
| **Multi-blog authors** | One main blog plus secondary blogs per user; platform paths and optional per-author subdomains. |
| **Editorial workflow** | Immutable publication snapshots, version history, editor-curated homepage, tag administration. |
| **Reader engagement** | Follow & email subscribe, reading list, text highlights & notes, threaded comments, in-app notifications, direct messages. |
| **Open web & syndication** | RSS (site, blog, tag, serie), ActivityPub (Mastodon-compatible), optional Git ↔ Jekyll sync. |
| **Production-ready ops** | PostgreSQL + Flyway, Docker/nginx deployment guide, Redis sessions, wildcard TLS for author subdomains. |

---

## Features

### Publishing

- **Multi-blog** — main + secondary blogs per user; public URLs by username and blog slug
- **Draft / publish** — working copy on `Post`; immutable `PostPublication` per publish; version history
- **Markdown & AsciiDoc** — server-side rendering with image library
- **Tags & series** — taxonomy, tag landing pages, serialized posts
- **Custom pages** — global, per-user main blog, or per secondary blog (`/page/...` URLs)
- **Git sync** — optional Jekyll-shaped repos per blog ([convention](docs/git-jekyll-convention.md))

### Reading & discovery

- **Featured homepage** — editor-curated hero and grid
- **Author & blog directories** — explore authors and blogs from the home page
- **Search** — HTMX modal + paginated `/search`
- **RSS** — site, blog, serie, and tag feeds
- **Reading list** — save posts for later; mark as read from the Reading hub
- **Highlights & notes** — select passages on posts; personal library in Reading hub
- **Comments** — threaded discussion with author moderation inbox

### Community & federation

- **Audience** — follow (in-app notifications) and email subscribe per blog
- **Notifications** — in-app inbox, badge, mark-as-read
- **Direct messages** — user-to-user mailbox with block list
- **Fediverse (ActivityPub)** — authors opt in to syndicate posts from **all blogs** to Mastodon and compatible servers ([feature doc](feature/activitypub-integration.md))
- **Share actions** — LinkedIn, Bluesky, copy link on posts and blogs

### Platform

- **Roles** — `USER`, `EDITOR`, `USER_ADMINISTRATOR`, `ADMIN`
- **Featured posts** — editors curate the homepage (`/editor/review`)
- **User administration** — platform operators manage accounts (`/administration/users`)
- **Author analytics** — dashboard views in Manage hub
- **i18n** — locale preference in header and account settings
- **HTMX chrome** — scoped refresh, toasts, modals ([htmx-events.md](docs/htmx-events.md))

Full UI route index with click paths: [docs/feature-catalog.md](docs/feature-catalog.md).

---

## Tech stack

| Layer | Technology |
|-------|------------|
| Backend | Quarkus (Java 17+) |
| Templates | Qute |
| UI dynamics | HTMX |
| Database | PostgreSQL, Hibernate ORM, Flyway |
| Cache / sessions | Redis (production) |
| Tests | JUnit 5, Selenium, `App` DSL + `Given` builders |
| Build | Maven |

---

## Quick start

**Prerequisites:** JDK 17+, PostgreSQL (or use Quarkus dev services).

```bash
git clone https://github.com/vepo/contraponto.git
cd contraponto
./mvnw quarkus:dev
```

Open [http://localhost:8080](http://localhost:8080). Dev mode runs Flyway clean+migrate (`%dev.quarkus.flyway.clean-at-start=true`) and loads sample data from [`dev-import.sql`](src/main/resources/dev-import.sql).

### Dev logins

All seeded users share the **admin** password (bcrypt hash in `dev-import.sql`).

| Username | Roles | Try |
|----------|-------|-----|
| `alice`, `bob`, `carol`, `vepo` | `USER` | Write, multi-blog URLs, library |
| `alice` | `USER` | ActivityPub actor, secondary blog `lab-notes` |
| `dave` | `USER` | Follower, notifications |
| `eve` | `USER` | Email subscriber |
| `editor` | `USER`, `EDITOR` | Featured curation `/editor/review`, tag admin |
| `admin` | `ADMIN`, `USER_ADMINISTRATOR` | `/administration/users`, welcome post |

Default admin (from migration): `admin` — rotate password after first deploy ([deployment checklist](docs/deployment.md)).

---

## Deploy to production

Step-by-step first deploy: [docs/greenfield-deployment-tutorial.md](docs/greenfield-deployment-tutorial.md)  
Environment variables: [docs/deployment.md](docs/deployment.md)  
Docker compose layout: sibling repo `contraponto-prod`.

---

## Documentation

| Doc | Contents |
|-----|----------|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Patterns, URL map, schema overview, how to add features |
| [docs/feature-catalog.md](docs/feature-catalog.md) | UI features, roles, navigation paths |
| [docs/domain-specification.md](docs/domain-specification.md) | Ubiquitous language, bounded contexts |
| [docs/htmx-events.md](docs/htmx-events.md) | HTMX lifecycle, scoped refresh, auth allowlist |
| [docs/adr/](docs/adr/) | Architecture Decision Records |
| [feature/](feature/) | Feature specs and changelog |
| [AGENTS.md](AGENTS.md) | Index for AI / Cursor agents |
| [docs/application-guidelines.md](docs/application-guidelines.md) | Route and flow descriptions |
| [docs/ui-guidelines.md](docs/ui-guidelines.md) | Typography, colors, components |
| [docs/git-jekyll-convention.md](docs/git-jekyll-convention.md) | Git repository layout for sync |
| [docs/greenfield-deployment-tutorial.md](docs/greenfield-deployment-tutorial.md) | First production deploy (server, DNS, TLS, Docker) |
| [docs/deployment.md](docs/deployment.md) | Production env-var checklist |
| [.cursor/rules/](.cursor/rules/) | Cursor project rules |

---

## Project structure

```
src/main/java/dev/vepo/contraponto/
├── activitypub/    # Fediverse federation (ActivityPub)
├── admin/          # Platform administration
├── auth/           # Authentication
├── blog/           # Blogs (multi per user), public blog pages
├── comment/        # Post comments
├── components/     # Header, menu, auth/publish forms
├── custompage/     # Static pages + cache filter
├── dashboard/      # Author dashboard
├── git/            # Git ↔ Jekyll import/export
├── highlight/      # Text highlights & notes
├── home/           # Featured homepage
├── image/          # Image upload API
├── library/        # Drafts & published lists
├── messaging/      # Direct messages
├── notification/   # Follow, subscribe, in-app + email
├── post/           # Posts, publication snapshots
├── profile/        # Profile settings
├── readinglist/    # Save for later
├── renderer/       # Markdown / AsciiDoc
├── rss/            # RSS feeds
├── search/         # Modal + full-page search
├── serie/          # Post series
├── shared/         # LoggedUser, pagination, infra
├── tag/            # Tag pages & curation
├── user/           # Users & roles
├── view/           # View counts
└── write/          # Editor
```

---

## Useful routes

| Area | Path |
|------|------|
| Home | `/` |
| Write | `/write`, `/write/draft/{id}` |
| Writing hub | `/writing`, `/writing/library`, `/writing/blogs`, `/writing/appearance` |
| Reading hub | `/reading`, `/reading/saved`, `/reading/highlights` |
| Manage hub | `/manage`, `/manage/dashboard`, `/manage/pages`, `/manage/comments` |
| Account hub | `/account`, `/account/security`, `/account/notifications`, `/account/messages` |
| Editor hub | `/editor`, `/editor/review`, `/editor/tags` |
| Administration | `/administration/users` |
| Search | `/search` |
| Blog edit | `/blogs/{id}/edit`, `/blogs/new` |
| Tags (public) | `/tags/{slug}` |
| Site RSS | `/feed` |

Post and blog URLs: `/{username}/post/{slug}`, `/{username}/{blogSlug}/post/{slug}`, etc. (see [ARCHITECTURE.md](ARCHITECTURE.md)).

---

## Testing

```bash
GITHUB_ACTIONS=true ./mvnw -B verify
```

- `@WebTest` — Quarkus + Selenium; inject `App` for navigation (headless when `GITHUB_ACTIONS=true`).
- `Given.user()`, `Given.post()`, `Given.blog()`, `Given.customPage()` for data setup.
- `Given.cleanup()` when tests leave shared data behind.

---

## Contributing

Contraponto uses a documented seven-phase development process (feature analysis → architecture → tasks → approval → TDD → review). Start from [feature/README.md](feature/README.md) and [development-process.mdc](.cursor/rules/development-process.mdc).  
AI-assisted workflows: [AGENTS.md](AGENTS.md).

---

## License

[GNU General Public License v2.0](LICENSE).

---

## Cursor / AI setup

Rules live in `.cursor/rules/` (`contraponto-core.mdc` always applies). Start from [AGENTS.md](AGENTS.md).
