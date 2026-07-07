# Session store (in-memory vs Redis)

> **Status**: Proposed
>
> **Updated**: 2026-07-07
>
> **Aceitação / reabertura:** somente **manual** pelo usuário humano.

## Summary

Authenticated sessions use a custom **`__session` cookie** mapped to user id via `SessionStore`. **Single-instance** deployments use `InMemorySessionStore`; **multi-instance** production uses `RedisSessionStore` when Redis is configured. Session is separate from Quarkus Security OIDC (local username/password accounts only).

## Drivers

* Horizontal scaling requires shared session state.
* Dev and CI should run without Redis.
* Blog subdomain cookie domain must align with `APP_SESSION_COOKIE_DOMAIN`.

## Options

### Pluggable SessionStore (implemented)

`SessionStoreProducer` selects implementation from configuration.

### Quarkus OIDC / JWT-only

Would not match current custom auth modal + form login UX.

### Sticky sessions only

Fragile under deploys and autoscaling.

## Options Analysis

### Pluggable SessionStore

* Pro: Same auth code paths in dev and prod; Redis optional.
* Con: Custom session layer (no built-in Quarkus session extension).

## Recommendation

Keep **`SessionStore` abstraction** with in-memory default and Redis for production multi-node.

### Consequences

* Pro: `LoggedUserProvider` unchanged across environments.
* Con: Session invalidation on password change must explicitly call `invalidateAllSessionsForUser` (partial gap on admin password change — see feature/authentication.md FQ4).

### Confirmation

* `LoginTest` passes in CI (in-memory).
* Production compose documents Redis + `APP_SESSION_COOKIE_DOMAIN`.

## Changelog

| Data | Evento | Detalhe |
|------|--------|---------|
| 2026-07-07 | proposed | Retroactive ADR documenting shipped session model. |

## More Information

* [feature/authentication.md](../../feature/authentication.md)
