# Deployment checklist

Greenfield production setup for Contraponto. The application is feature-complete for the [feature catalog](feature-catalog.md); this document covers **configuration and operations** required before real users.

**New environment?** Follow the step-by-step walkthrough first: [greenfield-deployment-tutorial.md](greenfield-deployment-tutorial.md) (server, DNS, TLS, compose, verification). Use **this file** as the env-var and optional-features reference while you deploy.

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
| `APP_BLOG_SUBDOMAIN_BASE_DOMAIN` | when subdomains enabled | Base domain for author blogs, e.g. `commit-mestre.dev` → public URLs at `https://{username}.commit-mestre.dev/post/{slug}` (requires `app.blog-subdomain.enabled=true`, set automatically in `%prod`) |
| `APP_SESSION_COOKIE_DOMAIN` | recommended with subdomains | Cookie `Domain` attribute for shared login across platform and author subdomains, e.g. `.commit-mestre.dev` |
| `APP_SITE_NAME` | no | White-label display name for header, footer, page titles, SEO, and emails (default `contraponto`; title-case derived for SEO, e.g. `commit-mestre` → Commit Mestre) |
| `APP_SITE_INTEGRATION_SCRIPT_URL` | no | HTTPS URL of an optional async script in page `<head>` (e.g. analytics); requires `APP_SITE_INTEGRATION_SCRIPT_DATA_TOKEN` |
| `APP_SITE_INTEGRATION_SCRIPT_DATA_TOKEN` | no | `data-token` attribute for the integration script; both integration env vars must be set to enable the tag |

### Author blog subdomains

When `APP_BLOG_SUBDOMAIN_BASE_DOMAIN` is set (enabled automatically in `%prod`), each author’s main blog is also served at `https://{username}.{base-domain}/` with posts at `/post/{slug}`. Secondary blogs use `https://{username}.{base-domain}/{blogSlug}/post/{slug}`. Platform-path URLs (`https://blogs.example/{username}/post/{slug}`) remain valid; canonical SEO URLs use the subdomain form.

**DNS:** point `*.your-base-domain` (wildcard) to the same host as the platform (`blogs.your-base-domain`).

**TLS:** a wildcard certificate for `*.your-base-domain` requires Let’s Encrypt **DNS-01** validation (HTTP-01 webroot cannot issue wildcard certs). With **Squarespace DNS**, use the manual DNS-01 flow in [`contraponto-prod/docs/MOP-DEPLOYMENT.md`](../../contraponto-prod/docs/MOP-DEPLOYMENT.md) (`init-letsencrypt.sh`, Squarespace `_acme-challenge` TXT records, `renew-wildcard-cert.sh` every ~60 days). Example with an automated DNS plugin elsewhere:

```bash
certbot certonly --dns-<provider> -d "*.commit-mestre.dev" -d "commit-mestre.dev"
```

Configure nginx (or your ingress) with `server_name blogs.example *.example`, forward `Host` and `X-Forwarded-Proto` to the app, and set `APP_SESSION_COOKIE_DOMAIN=.example` so sessions work on both platform and author subdomains.

Full URL mapping (platform vs author host, menu, components): [blog-subdomain-urls.md](blog-subdomain-urls.md).

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

### In-app notification retention

Read notifications are purged after **`app.notifications.retention.read-days`** (default **7**) from `read_at`. Unread notifications are purged after **`app.notifications.retention.unread-days`** (default **30**) from `created_at`. A scheduled job runs every **`app.notifications.retention.schedule`** (default **24h**). See [ADR-0010](adr/0010-notification-retention.md).

| Variable | Default | Purpose |
|----------|---------|---------|
| `APP_NOTIFICATIONS_RETENTION_READ_DAYS` | `7` | Days to keep read in-app notifications |
| `APP_NOTIFICATIONS_RETENTION_UNREAD_DAYS` | `30` | Days to keep unread in-app notifications |
| `APP_NOTIFICATIONS_RETENTION_SCHEDULE` | `24h` | Quarkus scheduler interval for purge job |

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

## 8. Optional: ActivityPub (Fediverse)

Disabled by default in prod (`contraponto.activitypub.enabled=false`). Flyway creates ActivityPub tables on first deploy of a release that includes them; federation remains off until configured.

To enable server-to-server federation (Mastodon and compatible apps):

| Variable | Required | Purpose |
|----------|:--------:|---------|
| `CONTRAPONTO_ACTIVITYPUB_ENABLED` | yes | `true` to allow authors to opt in |
| `CONTRAPONTO_ACTIVITYPUB_KEY_ENCRYPTION_SECRET` | yes | AES key material for encrypting actor private keys at rest — generate once (`openssl rand -base64 32`); **do not rotate** after actors exist |

Do **not** set `CONTRAPONTO_ACTIVITYPUB_INSECURE_ACCEPT_UNSIGNED=true` in production.

Authors enable federation per account under **Writing → Appearance**. Platform administrators can disable federation globally under **Platform insights**.

**Outbound HTTP Signatures:** deliveries set `Host` on the signed POST (same value as in the Signature header list). The JVM image sets `-Djdk.httpclient.allowRestrictedHeaders=host` at start ([Dockerfile.jvm](../src/main/docker/Dockerfile.jvm)). Local `./mvnw quarkus:dev` / IDE runs need the same flag if you exercise Delivery against a real remote.

**Operator procedure:** [contraponto-prod/docs/MOP-UPDATE.md](../../contraponto-prod/docs/MOP-UPDATE.md) §6.

**DNS / nginx:** author subdomains, **apex domain** (`commit-mestre.dev` for WebFinger), and `/.well-known/webfinger` must reach the app. Wildcard `*` does not cover the apex — add an apex **A** record to the same server. See [contraponto-prod/docs/MOP-DEPLOYMENT.md](../../contraponto-prod/docs/MOP-DEPLOYMENT.md) §2.2.

See [feature/activitypub-integration.md](feature/activitypub-integration.md) and ADRs [0006](adr/0006-activitypub-federation.md)–[0008](adr/0008-activitypub-actor-identity.md).

## 9. Build and run

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

## 10. Local development email

By default, `%dev` uses `quarkus.mailer.mock=true` (no outbound mail). For real SMTP in dev, copy [`application-dev.properties.example`](../src/main/resources/application-dev.properties.example) to `application-dev.properties` (gitignored) and fill in credentials.

## Related docs

- [ARCHITECTURE.md](../ARCHITECTURE.md) — patterns and configuration overview
- [README.md](../README.md) — quick start and test commands
- [greenfield-deployment-tutorial.md](greenfield-deployment-tutorial.md) — step-by-step first deploy on a new server/domain
