# HTMX events and scoped refresh

How Contraponto uses [HTMX](https://htmx.org/) lifecycle events, custom DOM events, and response headers. For stack context see [ARCHITECTURE.md](../ARCHITECTURE.md) §2.

## 1. Scoped refresh (core rule)

**Custom events are signals, not page reloads.** A broadcast on `body` (e.g. `loggedIn`) only causes **opt-in subscribers** to run their own small `hx-get` and swap **their own container**. `#main` and the current route stay untouched unless the user navigates explicitly.

### Pick the smallest mechanism

| Priority | Mechanism | When |
|----------|-----------|------|
| 1 | **Inline / OOB HTML** in the mutation response | The server already has the fragment (menu on login). |
| 2 | **`hx-target` on the activating element** | One region updates from one form (comment → list). |
| 3 | **Body broadcast + `hx-trigger` subscribers** | Several unrelated chrome/widgets must react (auth). Each subscriber declares `hx-get`, target, and `hx-swap`. |

`{"loggedIn":{"target":"body"}}` means “subscribers may listen on `body`,” **not** “reload the page.”

### Anti-patterns (forbidden)

- `window.location.reload()` or `hx-get="/"` with `hx-target="main"` after login/signup.
- A single global `loggedIn` JS handler that fetches many endpoints.
- Full layout HTML from `/forms/*` when a `/components/*` partial exists.
- Unscoped `htmx:afterSettle` handlers that always run `hljs.highlightAll()` without checking the swap target (see technical debt in `main.js`).

### When full navigation is allowed

- User clicks nav: `href` + `data-hx-get` (same URL) + `hx-select="main"` + `hx-target="main"` + `hx-swap="outerHTML"` + `hx-push-url`. Default `innerHTML` would nest `<main>` inside `<main>`; `main.js` enforces `outerHTML` when both select and target are `main`. After each main swap, `main.js` (`setupMainNavigationScroll`) calls `window.scrollTo(0, 0)` (skipped on history restore and hash/anchor URLs).
- **Ctrl/Cmd/Shift/middle-click** on those anchors: `main.js` (`setupModifierKeyNavigation`) cancels the HTMX request and opens `href` (or `hx-push-url`) in a new tab. Without `href`, the browser cannot open a new tab; without the handler, HTMX would still swap `#main` in place.
- **`loggedOut`** on a **protected path**: `main.js` redirects to `/` (documented exception).

### Author subdomain · workspace vs discovery routes

On `{username}.{base-domain}`, **author content** (`/`, `/post/…`, `/feed`, …) is rewritten to `/{username}/…`. **Workspace hubs** (`/writing`, `/manage`, `/administration`, `/account`, …) are served on the **same author host** without redirecting to the platform host — menu HTMX links stay same-origin.

**Discovery / global routes** (`/authors`, `/explore`, `/sitemap.xml`, …) still redirect to the platform host. For those, `BlogSubdomainFilter` responds to **`HX-Request: true`** with **`HX-Redirect`** (full-page navigation).

### SEO head sync (HTMX navigation)

Public page templates include `{#include components/seo-oob seo=seo /}`; `seo-oob` renders only when `HX-Request: true` so a normal GET has a single canonical in `<head>` (no duplicate tags in `<body>` for crawlers). HTMX navigations OOB-swap `#seo-head` (title, description, canonical, Open Graph, JSON-LD) when `main` is replaced.

On every `main` swap, `main.js` (`setupSeoSync`) refetches `GET /components/seo?path={pathname}` (OOB response updates `#page-title` and `#seo-head`; `hx-select="main"` does not apply OOB from the navigation response itself). **History back/forward** also refetches SEO (`htmx:historyRestore`) because `hx-history-elt` restores body only, not `<head>`.

Manage/auth surfaces pass `noindex` metadata via `SeoService.forPrivatePage(...)`.

### Page asset bundles (HTMX navigation)

Public pages load a minimal `<head>` bundle (`PageAssets` in [`PageAssetsFilter`](../src/main/java/dev/vepo/contraponto/shared/infra/PageAssetsFilter.java)). Write, post-reading, and manage assets are omitted on first paint when not needed.

Because HTMX swaps `#main` only, [`asset-loader.js`](../src/main/resources/META-INF/resources/js/asset-loader.js) injects missing bundles when:

- the user navigates to `/write`, a published post (`/post/`), or a manage hub path;
- or hovers a Write trigger (preload).

After dynamic injection, `asset-loader` dispatches `contraponto:assets-ready` with `{ profile: 'write' | 'post' | 'manage' }`. `write.js` mounts the editor on that event; `main.js` awaits post assets before running `hljs`.

---

## 2. Auth session change (login / signup / logout)

### Login / signup response layers

`LoginEndpoint` and `SignUpEndpoint` do **not** return a full page.

| Layer | What updates | Scope |
|-------|----------------|-------|
| Primary swap | `hx-target="#authError"` on the modal form | Modal errors only |
| OOB | `#menu-container` | Menu HTML |
| OOB | `#modal-container` cleared | Modal shell |
| `HX-Trigger-After-Settle` | `loggedIn` on `body` | Opt-in subscribers only |

`#main`, posts, grids, and library tabs are **not** refreshed.

### Logout

- OOB `#menu-container` in the logout response.
- `loggedOut` on `body` for other subscribers (write button, notification badge, in-page widgets).

### Auth-sensitive component allowlist

Update this table when adding auth-dependent UI.

| DOM id / pattern | Endpoint | OOB on login/logout | `loggedIn` / `loggedOut` subscriber |
|------------------|----------|---------------------|-------------------------------------|
| `#menu-container` | (included in page; OOB from `/forms/auth/*`) | Yes | No — menu comes from OOB only |
| write-btn wrapper | `GET /components/write-btn` | No | Yes |
| `#notification-badge-container` | `GET /components/notifications/badge` | No | `{notificationBadgeTrigger}` (`loggedIn`, `loggedOut`, `notificationsChanged`, poll) |
| `#notificationOverlay` | `GET /components/notifications/overlay` | No | Loaded on bell open; refreshed on `notificationsChanged` when open. `header.js` rebinds the bell only on `#notification-badge-container` swap, not on overlay content swap. |
| `#blog-audience-{blogId}` | `GET /components/blogs/{id}/audience` | No | Yes, if on page |
| `#comments` (post lazy-load shell) | `GET {post.url}/components/comments` | No | Yes, on post page |
| `#main-content` (subscriptions) | `GET /subscriptions` (select `main`) | No | Yes, on subscriptions page |
| `#main` | — | No | **Never** on auth events |

### Template attribute for subscribers

Use the shared trigger value (same string as `HtmxTriggers.AUTH_REFRESH_TRIGGER`):

```html
<div hx-get="/components/write-btn" hx-trigger="{authRefreshTrigger}">
```

Constants: `dev.vepo.contraponto.shared.htmx.HtmxTriggers` (Java), `{authRefreshTrigger}` and `{notificationBadgeTrigger}` via `Globals` (`@TemplateGlobal`).

Notification bell (`header.js`): toggles `#notificationOverlay`, shows placeholder (**No notification**) until `GET /components/notifications/overlay` completes, closes on outside click / Escape / nav link; re-fetches overlay when open on `notificationsChanged`. Menu rebind after badge **container** swap only (not after overlay innerHTML swap).

---

## 3. Application custom events

| Event | Producer | Consumers | Scope |
|-------|----------|-----------|-------|
| `toast:show` | `Toast.java` (`HX-Trigger` + `HX-Trigger-After-Settle`) | `toast.js` | `#toast` only |
| `loggedIn` | Login, SignUp (`HX-Trigger-After-Settle`) | Templates with `{authRefreshTrigger}`; not `#menu-container` | Allowlist only |
| `loggedOut` | Logout (`HX-Trigger-After-Settle`) | Same + `main.js` protected-path redirect | Chrome + optional nav to `/` |
| `notificationsChanged` | `MarkNotificationsReadEndpoint`, `DismissNotificationEndpoint` | `#notification-badge-container`; `header.js` reloads open overlay | Badge + overlay only |

### Naming for new events

- Format: **`{domain}:{action}`** (e.g. `toast:show`; future `auth:logged-in`).
- Legacy: `loggedIn` / `loggedOut` (camelCase, no domain) — do not rename without a coordinated migration.
- Java: add JSON to `HtmxTriggers`; never hand-build trigger strings in endpoints.
- Prefer OOB or direct swap over a new body broadcast when only one element changes.

### Response headers (parallel to DOM events)

| Header | Set by | Consumed by |
|--------|--------|-------------|
| `X-Toast-Message`, `X-Toast-Type`, `X-Toast-Duration` | `Toast.java` | `toast.js` on `htmx:afterRequest` / `afterSettle` |
| `HX-Trigger`, `HX-Trigger-After-Settle` | `Toast.java`, auth forms | HTMX + `toast.js` |
| `HX-Push-Url` | `Toast.url()`, some forms | HTMX history |

Always use `Toast.ok()...message()` for toasts — not raw `X-Toast-Message` in endpoints.

---

## 4. Backend rules

| Concern | Rule |
|---------|------|
| Scope | Return the smallest HTML: target fragment + optional `hx-swap-oob` |
| Toasts | `Toast` helper only |
| Auth | OOB `#menu-container` + `HtmxTriggers.LOGGED_IN_ON_BODY` / `LOGGED_OUT_ON_BODY` on `HX-Trigger-After-Settle` |
| New auth UI | Add a row to the allowlist (§2) + `{authRefreshTrigger}` on that container |
| Routes | `/forms/*` mutations, `/components/*` fragments |
| Never | Full page from `/forms/*`; unscoped `HX-Refresh` |

---

## 5. Frontend rules

| Concern | Rule |
|---------|------|
| Subscribers | `hx-get` + swap self; never fetch `/` or `#main` on auth events |
| Re-init JS | `htmx:afterSettle` / `afterSwap` with `evt.detail.target` guards |
| Global side effects | Toasts on `document` (capture) OK; redirects only for documented exceptions |
| Registration | One module per concern (`toast.js`, `main.js`, `reading-time.js`, `write.js`, …) |

### `reading-time.js` (post engagement)

| Event | Use |
|-------|-----|
| `DOMContentLoaded` / `htmx:afterSettle` | Start 5s heartbeat when `main.article-page[data-reading-time-post-id]` is present |
| `visibilitychange` | Pause interval when tab hidden; resume when visible |
| `htmx:beforeSwap` | Stop tracker when leaving post page (`#main-content` / `main` swap) |

### `write.js` (write editor)

| Event | Use |
|-------|-----|
| `DOMContentLoaded` | Create `window.writeEditor` singleton |
| `htmx:beforeSwap` | `unmount()` toolbar/tags listeners when `main` is replaced |
| `htmx:afterSettle` | `tryMount()` when swap target is `main` (or inside it) and `#editorToolbar` exists |
| `htmx:confirm` | Block HTMX navigation away from a **dirty** editor; show leave-confirmation modal |
| `htmx:afterRequest` | Reset dirty baseline after successful **Save draft**; resume pending navigation if any |
| `beforeunload` | Browser tab close / full reload guard when editor is dirty |

Mount uses element-reference identity (not `dataset.writeEditorBound` alone) so history restore and SPA re-entry never skip listener attachment or stack duplicate handlers.

### Lifecycle hooks used in this repo

| Event | Use in Contraponto |
|-------|-------------------|
| `htmx:afterRequest` | Read response headers; `hx-target-error` (`main.js`); toasts (`toast.js`) |
| `htmx:afterSwap` | Immediate chrome (sidebar, menu dropdown, cover upload) |
| `htmx:afterSettle` | Re-init widgets after transitions (forms, write editor, highlight.js) |
| `htmx:confirm` | Dirty write editor leave guard (`write.js`) |
| `htmx:configRequest` | Attach `Authorization` header (`authentication.js`) |
| `loggedOut` | Protected-path redirect (`main.js`) |

Full built-in catalog: [htmx.org reference — Events](https://htmx.org/reference/#events).

### `hx-trigger` cookbook

| Pattern | Example |
|---------|---------|
| Lazy load | `load` on element’s own `hx-get` |
| Auth chrome | `{authRefreshTrigger}` on each subscriber |
| Debounced search | `keyup changed delay:500ms, search` |

---

## 6. Built-in HTMX events (reference)

Shipped in `META-INF/resources/js/third-party/htmx.min.js`. Prefix: `htmx:`.

| Phase | Events |
|-------|--------|
| Init / DOM | `htmx:load`, `htmx:beforeProcessNode`, `htmx:afterProcessNode`, `htmx:beforeCleanupElement`, `htmx:trigger` |
| Request | `htmx:confirm`, `htmx:prompt`, `htmx:configRequest`, `htmx:validateUrl`, `htmx:beforeRequest`, `htmx:validation:validate`, `htmx:validation:failed`, `htmx:validation:halted`, `htmx:beforeSend`, `htmx:abort` |
| XHR | `htmx:xhr:loadstart`, `htmx:xhr:progress`, `htmx:xhr:abort`, `htmx:xhr:error`, `htmx:xhr:load`, `htmx:xhr:loadend`, `htmx:xhr:timeout` |
| Response | `htmx:afterRequest`, `htmx:afterOnLoad`, `htmx:beforeOnLoad` |
| Swap | `htmx:beforeSwap`, `htmx:afterSwap`, `htmx:afterSettle`, `htmx:beforeTransition`, `htmx:oobBeforeSwap`, `htmx:oobAfterSwap` |
| History | `htmx:beforeHistorySave`, `htmx:historyItemCreated`, `htmx:historyCacheHit`, `htmx:historyCacheMiss`, `htmx:historyCacheMissLoad`, `htmx:historyRestore`, `htmx:beforeHistoryUpdate`, `htmx:pushedIntoHistory`, `htmx:replacedInHistory`, `htmx:restored` |
| Errors | `htmx:swapError`, `htmx:responseError`, `htmx:sendError`, `htmx:sendAbort`, `htmx:timeout`, `htmx:targetError`, `htmx:onLoadError`, `htmx:oobErrorNoTarget`, `htmx:syntax:error`, `htmx:eventFilter:error`, `htmx:historyCacheError`, `htmx:historyCacheMissLoadError`, `htmx:badResponseUrl`, `htmx:invalidPath`, `htmx:evalDisallowedError`, `htmx:error` |

**Used in app JS today:** `htmx:afterRequest`, `htmx:afterOnLoad`, `htmx:afterSwap`, `htmx:afterSettle`, `htmx:configRequest`, `htmx:xhr:loadend`.

---

## 7. Tests

Assert contracts on REST tests where behavior matters:

- `HX-Trigger-After-Settle` for auth (`FollowAfterLoginTest`).
- `X-Toast-*` and `HX-Trigger` for mutations (`BlogSaveEndpointTest`).

Web tests use `App` toast helpers; no raw WebDriver for HTMX.
