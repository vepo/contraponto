# Blog subdomain URLs (dual public URL model)

> **Status**: Proposed
>
> **Updated**: 2026-07-07
>
> **Aceitação / reabertura:** somente **manual** pelo usuário humano.

## Summary

Contraponto serves the same application on a **platform host** (`blogs.example.com`) and **author blog subdomains** (`{username}.example.com`). Public content uses short paths on subdomains (`/post/{slug}`); workspace hubs (`/writing`, `/manage`, `/account`, …) redirect to the platform host. Canonical SEO, RSS, and email links prefer the subdomain form when `app.blog-subdomain.enabled=true`.

## Drivers

* Authors want short, memorable public URLs (`alice.example.com/post/my-slug`).
* Platform discovery and admin hubs stay on one origin for navigation consistency.
* Shared session across hosts requires cookie domain configuration (`APP_SESSION_COOKIE_DOMAIN`).

## Options

### Dual host with filter rewrite (implemented)

`BlogSubdomainFilter` maps subdomain requests to internal `/{username}/…` routes; workspace paths redirect to platform host.

### Platform paths only

Single host; all URLs include `/{username}/`.

### Separate static site per author

Out of scope for modular monolith.

## Options Analysis

### Dual host with filter rewrite

* Pro: Short public URLs; single deployable; workspace stays centralized.
* Con: Filter complexity; cross-host redirects; dev often disables subdomain.

## Recommendation

Keep **dual host with `BlogSubdomainFilter` + `BlogPublicUrlService`** for canonical URLs. Document operator config in [blog-subdomain-urls.md](../blog-subdomain-urls.md).

### Consequences

* Pro: Fediverse WebFinger and ActivityPub actor URLs align with author subdomain.
* Con: HTMX workspace navigation uses absolute platform URLs on author hosts.

### Confirmation

* `BlogSubdomainIntegrationTest`, `BlogPublicUrlServiceTest` green.
* Production compose sets `APP_BLOG_SUBDOMAIN_BASE_DOMAIN` and session cookie domain.

## Changelog

| Data | Evento | Detalhe |
|------|--------|---------|
| 2026-07-07 | proposed | Retroactive ADR documenting shipped dual-URL model. |

## More Information

* [blog-subdomain-urls.md](../blog-subdomain-urls.md)
* [feature/multi-blog.md](../../feature/multi-blog.md)
