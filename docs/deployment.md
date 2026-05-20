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
| `APP_PUBLIC_URL` | yes | HTTPS origin, e.g. `https://contraponto.example` — used in emails, RSS links, and HTML sanitization (`image.base.url`) |

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

## 4. Security

| Item | Action |
|------|--------|
| **HTTPS** | Terminate TLS at the edge; `%prod` sets `app.secure-cookies=true` for session and view cookies |
| **Bootstrap admin** | Migration seeds user `admin` with a known bcrypt hash — **change the password immediately** after first login |
| **Secrets** | Never commit SMTP or DB passwords; use platform secret stores |
| **Dev import** | `app.dev-import.enabled=false` in prod (default) |

## 5. Optional: Git / Jekyll sync

Disabled by default in prod (`%prod.contraponto.git.poll-enabled=false`). To enable:

1. Set `contraponto.git.poll-enabled=true` (or override in env)
2. Mount a **persistent volume** at `contraponto.git.workspace-root` (default is ephemeral under `java.io.tmpdir`)
3. Set `contraponto.git.username` / `contraponto.git.password` (or PAT in remote URL) for HTTPS remotes

See [git-jekyll-convention.md](git-jekyll-convention.md).

## 6. Optional: Redis session store

For multiple app instances, set `app.session.store=redis` and configure `quarkus.redis.hosts`. Default is in-memory (`app.session.store=memory`), suitable for a single node.

## 7. Build and run

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

## 8. Local development email

By default, `%dev` uses `quarkus.mailer.mock=true` (no outbound mail). For real SMTP in dev, copy [`application-dev.properties.example`](../src/main/resources/application-dev.properties.example) to `application-dev.properties` (gitignored) and fill in credentials.

## Related docs

- [ARCHITECTURE.md](../ARCHITECTURE.md) — patterns and configuration overview
- [README.md](../README.md) — quick start and test commands
