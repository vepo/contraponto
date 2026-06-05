# Deployment checklist

Greenfield production setup for Contraponto. The application is feature-complete for the [feature catalog](feature-catalog.md); this document covers **configuration and operations** required before real users.

## Prerequisites

- Java 25 runtime (or use [Dockerfile.jvm](../src/main/docker/Dockerfile.jvm) after `mvn package`; native image uses [Dockerfile](../src/main/docker/Dockerfile) with `mvn package -Pnative`)
- PostgreSQL 14+
- HTTPS termination (reverse proxy or platform ingress)
- SMTP server for transactional and notification email

## 1. Database

Set standard Quarkus datasource environment variables:

| Variable | Example |
|----------|---------|
| `QUARKUS_DATASOURCE_JDBC_URL` | `jdbc:postgresql://db:5432/contraponto` |
| `QUARKUS_DATASOURCE_USERNAME` | `contraponto` |
| `QUARKUS_DATASOURCE_PASSWORD` | *(secret)* |

On first start, Flyway applies a single migration: [`V0.0.1__initial_schema.sql`](../src/main/resources/db/migration/V0.0.1__initial_schema.sql).

**Greenfield only:** do not point this release at a database that already ran older `V1.0.x` Flyway scripts (history mismatch). Use a fresh database or restore from backup after a planned migration.

**Backups:** image bytes are stored in PostgreSQL (`tb_image_content`). Size backups accordingly; there is no external object store.

## 2. Application profile and public URL

Run with production profile:

```bash
export QUARKUS_PROFILE=prod
```

| Variable | Required | Purpose |
|----------|:--------:|---------|
| `APP_PUBLIC_URL` | yes | HTTPS origin, e.g. `https://contraponto.example` — used in emails, RSS links, HTML sanitization, SEO canonical/Open Graph URLs, and `Sitemap:` in `robots.txt` (`image.base.url`) |
| `APP_SITE_NAME` | no | White-label display name for header, footer, page titles, SEO, and emails (default `contraponto`; title-case derived for SEO, e.g. `commit-mestre` → Commit Mestre) |
| `APP_SITE_INTEGRATION_SCRIPT_URL` | no | HTTPS URL of an optional async script in page `<head>` (e.g. analytics); requires `APP_SITE_INTEGRATION_SCRIPT_DATA_TOKEN` |
| `APP_SITE_INTEGRATION_SCRIPT_DATA_TOKEN` | no | `data-token` attribute for the integration script; both integration env vars must be set to enable the tag |

## 3. SMTP (mailer)

Configure outbound mail (signup verification, password reset, post notifications to subscribers):

| Variable | Purpose |
|----------|---------|
| `QUARKUS_MAILER_HOST` | SMTP host |
| `QUARKUS_MAILER_PORT` | Port (e.g. `587`) |
| `QUARKUS_MAILER_START_TLS` | `REQUIRED` or `OPTIONAL` as appropriate |
| `QUARKUS_MAILER_USERNAME` | Auth user (if needed) |
| `QUARKUS_MAILER_PASSWORD` | Auth password (secret) |
| `QUARKUS_MAILER_FROM` | From address (default in `application.properties`: `noreply@contraponto.blog`) |
| `APP_ADMIN_NOTIFY_EMAIL` | Optional comma-separated administrator inbox(es) for unauthorized signup reports (`app.admin.notify-email`; when unset, active `ADMIN` / `USER_ADMINISTRATOR` users are notified) |

## 4. Search indexing (production)

After the app is reachable on the public HTTPS origin:

1. Confirm **`APP_PUBLIC_URL`** matches the canonical host (no mixed `www` / non-`www`, no `http://` in sitemap locs).
2. Open [Google Search Console](https://search.google.com/search-console), verify the property, and submit **`{APP_PUBLIC_URL}/sitemap.xml`**.
3. Spot-check `GET /robots.txt`, `GET /sitemap.xml`, and a published post in “URL inspection” (canonical, JSON-LD, `article:modified_time` on republished posts).
4. Optional: use [Rich Results Test](https://search.google.com/test/rich-results) on home and a post URL.

## 5. Security

| Item | Action |
|------|--------|
| **HTTPS** | Terminate TLS at the edge; `%prod` sets `app.secure-cookies=true` for session and view cookies |
| **Bootstrap admin** | Migration seeds user `admin` with a known bcrypt hash — **change the password immediately** after first login |
| **Secrets** | Never commit SMTP or DB passwords; use platform secret stores |
| **Dev import** | `app.dev-import.enabled=false` in prod (default) |

## 6. Optional: Git / Jekyll sync

Disabled by default in prod (`%prod.contraponto.git.poll-enabled=false`). To enable:

1. Set `contraponto.git.poll-enabled=true` (or override in env)
2. Mount a **persistent volume** at `contraponto.git.workspace-root` (default is ephemeral under `java.io.tmpdir`)
3. Set `contraponto.git.username` / `contraponto.git.password` (or PAT in remote URL) for HTTPS remotes

See [git-jekyll-convention.md](git-jekyll-convention.md).

## 7. Optional: Redis session store

For multiple app instances, set `app.session.store=redis` and configure `quarkus.redis.hosts`. Default is in-memory (`app.session.store=memory`), suitable for a single node.

## 8. Build and run

```bash
mvn package -DskipTests
docker build -f src/main/docker/Dockerfile.jvm -t contraponto .
docker run -p 8080:8080 \
  -e QUARKUS_PROFILE=prod \
  -e APP_PUBLIC_URL=https://your.domain \
  -e QUARKUS_DATASOURCE_JDBC_URL=... \
  -e QUARKUS_DATASOURCE_USERNAME=... \
  -e QUARKUS_DATASOURCE_PASSWORD=... \
  -e QUARKUS_MAILER_HOST=... \
  contraponto
```

**Health:** `GET /q/health` (SmallRye Health).

## 9. Local development email

By default, `%dev` uses `quarkus.mailer.mock=true` (no outbound mail). For real SMTP in dev, copy [`application-dev.properties.example`](../src/main/resources/application-dev.properties.example) to `application-dev.properties` (gitignored) and fill in credentials.

## Related docs

- [ARCHITECTURE.md](../ARCHITECTURE.md) — patterns and configuration overview
- [README.md](../README.md) — quick start and test commands
