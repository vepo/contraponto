# Docker smoke stack (prod-faithful compose)

Prod-faithful compose for CI **`docker-smoke`** job and local `-Ptest-it` runs. The test harness starts the stack with **`docker compose`** (CLI), not the Testcontainers Java Compose module — same service graph as production.

**Canonical production:** [`contraponto-prod/docker-compose.yml`](../../../../contraponto-prod/docker-compose.yml) and [`contraponto-prod/data/nginx/app.conf`](../../../../contraponto-prod/data/nginx/app.conf).

When prod compose, nginx, or `%prod` env vars change, update this directory and [`docs/greenfield-deployment-tutorial.md`](../../../../docs/greenfield-deployment-tutorial.md) in the same PR (see [`.cursor/rules/docker-smoke-prod-sync.mdc`](../../../../.cursor/rules/docker-smoke-prod-sync.mdc)).

## Host mapping

| Role | Smoke | Production |
|------|-------|------------|
| Platform | `blogs.commit-mestre.test` | `blogs.commit-mestre.dev` |
| Author (`admin`) | `admin.commit-mestre.test` | `admin.commit-mestre.dev` |
| Base domain | `commit-mestre.test` | `commit-mestre.dev` |

Add to `/etc/hosts` before running smoke tests:

```bash
echo "127.0.0.1 blogs.commit-mestre.test admin.commit-mestre.test" | sudo tee -a /etc/hosts
```

## Prod ↔ smoke environment mapping

| Variable | Production | Smoke |
|----------|------------|-------|
| `QUARKUS_PROFILE` | `prod` | `prod` |
| `APP_SESSION_STORE` | `redis` | `redis` |
| `APP_ADMIN_NOTIFY_EMAIL` | operator email | `smoke@contraponto.test` |
| `QUARKUS_REDIS_HOSTS` | `redis://redis:6379` | same |
| `QUARKUS_DATASOURCE_*` | prod secrets | fixed smoke password |
| `QUARKUS_HIBERNATE_ORM_DATABASE_GENERATION` | `validate` | `validate` |
| `MP_JWT_VERIFY_PUBLICKEY_LOCATION` | `file:/opt/keys/public_key.pem` | same |
| `IMAGE_BASE_URL` | `https://blogs.commit-mestre.dev/` | `http://blogs.commit-mestre.test:8080/` |
| `APP_PUBLIC_URL` | `https://blogs.commit-mestre.dev` | `http://blogs.commit-mestre.test:8080` |
| `APP_BLOG_SUBDOMAIN_BASE_DOMAIN` | `commit-mestre.dev` | `commit-mestre.test` |
| `APP_SESSION_COOKIE_DOMAIN` | `.commit-mestre.dev` | `.commit-mestre.test` |
| `APP_SITE_NAME` | `commit-mestre` | `commit-mestre` |
| `APP_SITE_INTEGRATION_SCRIPT_*` | prod analytics | omitted |
| `PASSWORD_SALT` | prod secret | fixed smoke value |
| `QUARKUS_MAILER_*` | Mailtrap SMTP | `QUARKUS_MAILER_MOCK=true` |
| `APP_SECURE_COOKIES` | `%prod` true | `false` (HTTP) |
| `QUARKUS_HTTP_PROXY_*` | `%prod` true | explicit `true` |

## Services

| Service | Prod | Smoke delta |
|---------|------|-------------|
| `postgres` | persistent volume | ephemeral |
| `redis` | same | same |
| `contraponto` | `vepo/contraponto:main` | `${CONTRAPONTO_IMAGE}` |
| `nginx` | TLS + certbot | HTTP on host port **8080** (avoids privileged port 80 in CI) |
| `certbot` | present | omitted |

## Local run

Default `mvn verify` runs Surefire only (`skipITs=true`) and excludes the `docker-smoke` tag. Integration tests use Failsafe with `-Ptest-it` (tag filter defaults to `docker-smoke`; override with `-Dit.groups=…`).

```bash
mvn package -DskipTests
docker build -f src/main/docker/Dockerfile.jvm -t contraponto:ci-smoke .
GITHUB_ACTIONS=true mvn -B verify -Ptest-it -Dcontraponto.smoke.image=contraponto:ci-smoke
```

JWT public key: `keys/public_key.pem` (generate with [`contraponto-prod/keys/generate-keys.sh`](../../../../contraponto-prod/keys/generate-keys.sh); do not commit `private_key.pem`).
