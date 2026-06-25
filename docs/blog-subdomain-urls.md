# Blog subdomain URL patterns

How Contraponto serves the same application on two host shapes:

| Host | Role | Example |
|------|------|---------|
| **Platform host** | Discovery, home, and canonical workspace entry | `https://blogs.commit-mestre.dev` |
| **Author blog subdomain** | Short public URLs for one author’s main blog | `https://vepo.commit-mestre.dev` |

Configuration (production):

| Setting | Example | Purpose |
|---------|---------|---------|
| `APP_PUBLIC_URL` | `https://blogs.commit-mestre.dev` | Platform origin; emails, RSS, redirects |
| `APP_BLOG_SUBDOMAIN_BASE_DOMAIN` | `commit-mestre.dev` | Enables `{username}.{base-domain}` |
| `APP_SESSION_COOKIE_DOMAIN` | `.commit-mestre.dev` | Shared login on platform + author hosts |

Implementation: `BlogSubdomainConfig`, `BlogSubdomainFilter`, `BlogPublicUrlService`.

---

## Platform host (`blogs.{base-domain}`)

Behaves like a single-site deployment. Paths are unchanged:

| Resource | URL |
|----------|-----|
| Home / discovery | `/` |
| Author main blog | `/{username}` |
| Main blog post | `/{username}/post/{slug}` |
| Secondary blog | `/{username}/{blogSlug}` |
| Secondary post | `/{username}/{blogSlug}/post/{slug}` |
| Workspace hubs | `/writing`, `/manage`, `/account`, `/editor`, `/administration`, … |
| HTMX shell partials | `/components/menu`, `/components/seo`, … |

The user menu and every hub section work at these paths on the platform host.

---

## Author blog subdomain (`{username}.{base-domain}`)

The filter treats the host label as the author **username** and maps **short public paths** to the same internal routes the platform uses.

### Public (short) URLs

| Resource | Subdomain URL | Internal route |
|----------|---------------|----------------|
| Main blog home | `/` | `/{username}` |
| Main blog post | `/post/{slug}` | `/{username}/post/{slug}` |
| Secondary blog | `/{blogSlug}` | `/{username}/{blogSlug}` |
| Secondary post | `/{blogSlug}/post/{slug}` | `/{username}/{blogSlug}/post/{slug}` |
| RSS (main) | `/feed` | `/{username}/feed` |
| Custom page | `/page/{slug}` | `/{username}/page/{slug}` |
| Load more (main blog) | `/components/grid` | `/{username}/components/grid` |

Platform-style links that include the username (`/{username}`, `/{username}/post/…`) are **normalized** on the matching subdomain so they do not double-prefix (e.g. `https://vepo.commit-mestre.dev/vepo` → main blog home).

### Workspace (same host, no redirect)

Logged-in hubs stay on the author host so HTMX navigation remains same-origin:

| Hub | Entry | Example sections |
|-----|-------|------------------|
| Writing | `/writing` | `/writing/library`, `/writing/blogs`, `/writing/images`, … |
| Reading | `/reading` | `/reading/saved`, `/reading/highlights`, `/reading/notes` |
| Manage | `/manage` | `/manage/dashboard`, `/manage/pages`, `/manage/comments`, … |
| Account | `/account` | `/account/notifications`, `/account/security`, … |
| Review (editors) | `/editor` | `/editor/review`, `/editor/tags` |
| Administration | `/administration` | `/administration/users`, `/administration/insights` |

Also served without redirect: `/write`, `/library`, `/search`, `/profile`, static assets (`/js`, `/style`, `/images`), auth modals (`/auth/modal`), and form posts (`/forms/…`).

### Global shell partials (no username prefix)

These paths are identical on every host and must **not** be rewritten with `/{username}`:

- `/components/menu`
- `/components/notifications/*`
- `/components/seo`
- `/components/write-btn`
- `/components/confirm-modal/*`
- `/components/avatar`
- `/components/images/*`
- `/components/blogs/*`
- `/components/home/*`

Author-scoped pagination **`/components/grid`** is the exception: on a subdomain it is rewritten to `/{username}/components/grid`.

### Discovery routes (redirect to platform)

Global discovery still opens on the platform host:

- `/authors`, `/explore`
- `/sitemap.xml`, `/robots.txt`, OpenAPI

HTMX requests receive `HX-Redirect` to `https://blogs.{base-domain}{path}`; full page loads use HTTP 302.

---

## Dual URL & canonical links

When subdomains are enabled, every published main-blog post has two valid URLs:

- Platform: `https://blogs.commit-mestre.dev/vepo/post/hello-world`
- Subdomain: `https://vepo.commit-mestre.dev/post/hello-world`

SEO, sitemap, RSS, and email use the **subdomain canonical** via `BlogPublicUrlService.absoluteCanonical`.

---

## Session & menu

With `APP_SESSION_COOKIE_DOMAIN=.commit-mestre.dev`, logging in on either host keeps the session on the other.

The user menu uses relative paths (`/writing`, `/manage`, `/components/menu`, …). On an author subdomain those paths are handled as described above so menu items and post-login chrome (menu refresh, notification badge, SEO sync) work without leaving the host.

---

## Verification checklist

After deploy, with a user who has editor and administrator roles:

1. Log in on `https://blogs.commit-mestre.dev`.
2. Open each user-menu hub and at least one section per hub — expect HTTP 200.
3. Repeat on `https://{username}.commit-mestre.dev` (same session after cookie domain is set).
4. On the author subdomain, confirm:
   - `GET /components/menu` → 200
   - `GET /{username}` (menu “Meu blog”) → 200, no redirect loop
   - `GET /post/{slug}` → 200

Related: [deployment.md](deployment.md), [htmx-events.md](htmx-events.md#author-subdomain--workspace-vs-discovery-routes), [greenfield-deployment-tutorial.md](greenfield-deployment-tutorial.md) §5.3.
