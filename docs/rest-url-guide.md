# REST URL Guide

Canonical reference for Contraponto HTTP paths, classified by **segment role** under Representational State Transfer (REST). Every URL transfers or requests a **representation** of application state; the HTTP **verb** is the primary operator, with path segments naming resources and scoping intent.

**Related:** [ARCHITECTURE.md](../ARCHITECTURE.md) §3 (public URL patterns), [blog-subdomain-urls.md](blog-subdomain-urls.md) (author subdomain aliases), [application-guidelines.md](application-guidelines.md) (UX flows), [feature-catalog.md](feature-catalog.md) (click paths).

**Last verified:** 2026-07-07 — against `*Endpoint` classes in `src/main/java`.

---

## 1. Segment taxonomy

Each path segment (slash-separated token) plays one primary role. Dynamic tokens (`{username}`, `{slug}`, `{id}`, …) are always **Identifiers**.

| Role | Meaning | REST role | Examples |
|------|---------|-----------|----------|
| **Grouping** | Namespace that scopes workspace, transport, or UI shell — not a domain entity | Context / gateway | `forms`, `components`, `api`, `account`, `writing`, `manage`, `reading`, `editor`, `administration`, `auth`, `_custom_page`, `.well-known` |
| **Resource** | Noun: entity or collection whose state is transferred | Resource name | `posts`, `blogs`, `users`, `tags`, `messages`, `threads`, `comments`, `highlights`, `notifications`, `feed`, `search`, `page`, `serie`, `images`, `inbox`, `outbox`, `activities` |
| **Identifier** | Instance key within a resource (path param or fixed literal picking one item) | Resource identity | `{username}`, `{blogSlug}`, `{slug}`, `{id}`, `{postId}`, `{uuid}`, `{threadId}`, `{tab}`, `open`, `closed` |
| **Info** | View, format, or metadata about the representation — not a state change | Representation qualifier | `components`, `grid`, `modal`, `analytics`, `history`, `badge`, `overlay`, `results`, `edit`, `new`, `settings`, `compose`, `feed`, `main-blog`, `tab`, `blocked`, `security`, `appearance` |
| **Action** | Named state transition when the path must express intent beyond the HTTP verb (common with HTML forms) | Sub-resource action | `approve`, `reject`, `dismiss`, `unpublish`, `follow`, `subscribe`, `toggle`, `close`, `flag`, `reply`, `unblock`, `read`, `unread`, `revoke`, `publish`, `draft`, `login`, `logout`, `signup`, `reset`, `request`, `reviewed`, `accept`, `deactivate` |

### 1.1 HTTP verb (primary action)

The **verb** distinguishes the transfer operation. Path **Action** segments refine intent when a single resource URL would be ambiguous (especially `POST` form targets).

| Verb | REST intent | Typical response |
|------|-------------|------------------|
| `GET` | Retrieve representation | `200` HTML, JSON, XML, or `activity+json` |
| `POST` | Create resource or invoke action | `200`/`303` fragment or redirect; toast via `X-Toast-Message` |
| `PUT` | Replace or idempotent update | `200`/`204` |
| `DELETE` | Remove resource | `200`/`204` |

**Convention:** Safe reads use bare paths or `GET /components/...`. Mutations use `POST`/`PUT`/`DELETE` under **`/forms/...`** (Grouping). HTMX partials use **`/components/...`** (Grouping + Info).

### 1.2 Segment notation in tables

Tables use a compact breakdown column:

```
G:forms  R:posts  I:{postId}  A:unpublish
```

Omitted roles mean the segment does not appear in that URL. Literals that are both resource and identifier (e.g. `post` in `.../post/{slug}`) are shown as **R:post I:{slug}**.

### 1.3 Author subdomain aliases

On `{username}.{base-domain}`, public paths shorten per [blog-subdomain-urls.md](blog-subdomain-urls.md). Internal routes and segment roles are unchanged after `BlogSubdomainFilter` rewrite.

| Platform path | Subdomain alias |
|---------------|-----------------|
| `/{username}` | `/` |
| `/{username}/post/{slug}` | `/post/{slug}` |
| `/{username}/{blogSlug}` | `/{blogSlug}` |
| `/{username}/{blogSlug}/post/{slug}` | `/{blogSlug}/post/{slug}` |
| `/{username}/feed` | `/feed` |
| `/{username}/page/{slug}` | `/page/{slug}` |

---

## 2. Top-level grouping prefixes

| Prefix | Role | Purpose |
|--------|------|---------|
| `/` | — | Home, catch-all author routes |
| `/forms` | Grouping | State-changing form posts (HTMX / full page) |
| `/components` | Grouping + Info | HTMX HTML fragments |
| `/api` | Grouping | JSON/binary API (`/api/images`) |
| `/account` | Grouping | Signed-in account workspace |
| `/writing` | Grouping | Author workspace |
| `/reading` | Grouping | Reader workspace |
| `/manage` | Grouping | Blog owner management hub |
| `/editor` | Grouping | Editor curation hub |
| `/administration` | Grouping | Platform admin hub |
| `/auth` | Grouping | Auth modals |
| `/write` | Resource | Composer (draft editor) |
| `/search` | Resource | Search |
| `/tags` | Resource | Tag taxonomy pages |
| `/authors` | Resource | Author directory |
| `/explore` | Grouping | Discovery |
| `/blogs`, `/users`, `/pages` | Resource | CRUD manage surfaces (full page) |
| `/i18n` | Grouping | Locale message bundles |
| `/_custom_page` | Grouping | Internal custom page resolver (after `CustomPageFilter` rewrite) |
| `/.well-known` | Grouping | Federation discovery |

---

## 3. Public discovery and reading

### 3.1 Platform home and directories

| Verb | URL | Segments | Notes |
|------|-----|----------|-------|
| GET | `/` | — | Featured home |
| GET | `/components/home/grid` | G:components R:home I:grid | HTMX load more |
| GET | `/authors` | R:authors | Author directory |
| GET | `/authors/{username}` | R:authors I:{username} | Author profile |
| GET | `/explore/blogs` | G:explore R:blogs | Blog directory |
| GET | `/search` | R:search | Advanced search page |
| GET | `/search/modal` | R:search I:modal | Quick search modal |
| GET | `/search/results` | R:search I:results | Search results fragment |
| GET | `/feed` | R:feed | Site-wide RSS |
| GET | `/highlights` | R:highlights | Legacy redirect → `/reading/highlights` |

### 3.2 Tags and series

| Verb | URL | Segments | Notes |
|------|-----|----------|-------|
| GET | `/tags/{slug}` | R:tags I:{slug} | Tag page |
| GET | `/tags/{slug}/components/grid` | R:tags I:{slug} I:grid | HTMX grid |
| GET | `/tags/{slug}/feed` | R:tags I:{slug} R:feed | Tag RSS |
| GET | `/tags/{slug}/edit` | R:tags I:{slug} I:edit | Editor metadata form |
| GET | `/{username}/serie/{serieSlug}` | I:{username} R:serie I:{serieSlug} | Main blog serie |
| GET | `/{username}/{blogSlug}/serie/{serieSlug}` | I:{username} I:{blogSlug} R:serie I:{serieSlug} | Secondary blog serie |

### 3.3 Author blogs and posts

| Verb | URL | Segments | Notes |
|------|-----|----------|-------|
| GET | `/{username}` | I:{username} | Main blog home (or profile if multi-blog) |
| GET | `/{username}/components/grid` | I:{username} I:grid | Main blog load more |
| GET | `/{username}/{blogSlug}` | I:{username} I:{blogSlug} | Secondary blog home |
| GET | `/{username}/{blogSlug}/components/grid` | I:{username} I:{blogSlug} I:grid | Secondary load more |
| GET | `/{username}/post/{slug}` | I:{username} R:post I:{slug} | Main blog post |
| GET | `/{username}/{blogSlug}/post/{slug}` | I:{username} I:{blogSlug} R:post I:{slug} | Secondary post |
| GET | `.../components/history` | … I:history | Version diff fragment |
| GET | `.../components/history/modal` | … I:history I:modal | History modal |
| PUT | `.../component/featured/toggle` | … I:featured A:toggle | Editor featured flag |

### 3.4 Custom pages (public)

`CustomPageFilter` rewrites public URLs to `/_custom_page/...`.

| Verb | Public URL | Internal URL | Segments (public) |
|------|------------|--------------|-------------------|
| GET | `/page/{slug}` | `/_custom_page/global/{slug}` | R:page I:{slug} |
| GET | `/{username}/page/{slug}` | `/_custom_page/user/{username}/{slug}` | I:{username} R:page I:{slug} |
| GET | `/{username}/{blogSlug}/page/{slug}` | `/_custom_page/blog/{username}/{blogSlug}/{slug}` | I:{username} I:{blogSlug} R:page I:{slug} |

### 3.5 RSS

| Verb | URL | Segments |
|------|-----|----------|
| GET | `/{username}/feed` | I:{username} R:feed |
| GET | `/{username}/feed/main-blog` | I:{username} R:feed I:main-blog |
| GET | `/{username}/{blogSlug}/feed` | I:{username} I:{blogSlug} R:feed |
| GET | `/{username}/serie/{serieSlug}/feed` | I:{username} R:serie I:{serieSlug} R:feed |
| GET | `/{username}/{blogSlug}/serie/{serieSlug}/feed` | I:{username} I:{blogSlug} R:serie I:{serieSlug} R:feed |

### 3.6 Post interaction components (HTMX)

Pattern: `/{username}[/ {blogSlug}]/post/{slug}/components/{facet}`

| Facet (`R` + `I`) | URL suffix | Verb |
|-------------------|------------|------|
| `comments` | `.../components/comments` | GET |
| `comments/{commentId}/replies` | `.../components/comments/{commentId}/replies` | GET |
| `highlights` | `.../components/highlights` | GET |
| `reading-list` | `.../components/reading-list` | GET |

### 3.7 SEO and crawlers

| Verb | URL | Segments |
|------|-----|----------|
| GET | `/sitemap.xml` | R:sitemap (file) |
| GET | `/robots.txt` | R:robots (file) |
| GET | `/components/seo` | G:components R:seo | Query: `path` |

---

## 4. Workspace hubs

Hub shells: `GET /{hub}` and `GET /{hub}/{section}`. Section slug is **Identifier**; hub name is **Grouping**.

| Hub (Grouping) | Default section | Sections |
|----------------|-----------------|----------|
| `/writing` | `library` | `library`, `images`, `blogs`, `highlights`, `appearance` |
| `/reading` | `highlights` | `saved`, `highlights`, `notes` |
| `/manage` | `dashboard` | `dashboard`, `blogs`, `pages`, `comments` |
| `/account` | `notifications` | `messages`, `notifications`, `subscriptions`, `security`; blocked users at `/account/messages/blocked` |
| `/editor` | `review` | `review`, `tags` |
| `/administration` | `users` | `users`, `message-reports`, `activitypub`, `insights` |

| Verb | URL | Segments | Notes |
|------|-----|----------|-------|
| GET | `/write` | R:write | New draft composer |
| GET | `/write/draft/{draftId}` | R:write R:draft I:{draftId} | Edit draft |
| GET | `/writing/library/components/tab/{type}` | G:writing R:library I:tab I:{type} | `drafts` \| `published` |
| GET | `/reading/saved/components/tab/{tab}` | G:reading R:saved I:tab I:{tab} | Reading list tabs |
| GET | `/manage/dashboard/components/analytics` | G:manage R:dashboard I:analytics | Dashboard metrics |
| GET | `/administration/insights/components/analytics` | G:administration R:insights I:analytics | Platform insights |
| GET | `/editor/review/components` | G:editor R:review | Featured review grid |
| PUT | `/editor/review/components/{postId}/featured/toggle` | … I:{postId} I:featured A:toggle | Curate featured |

### 4.1 Full-page manage (outside hub shell)

| Verb | URL | Segments |
|------|-----|----------|
| GET | `/blogs` | R:blogs |
| GET | `/blogs/new` | R:blogs I:new |
| GET | `/blogs/{id}/edit` | R:blogs I:{id} I:edit |
| GET | `/blogs/{id}/settings` | R:blogs I:{id} I:settings |
| GET | `/blogs/{blogId}/git-sync` | R:blogs I:{blogId} R:git-sync |
| GET | `/blogs/{blogId}/git-sync/{runId}` | … I:{runId} |
| GET | `/blogs/{blogId}/images` | R:blogs I:{blogId} R:images | Redirect → `/writing/images` |
| GET | `/users` | R:users |
| GET | `/users/new` | R:users I:new |
| GET | `/users/{id}/edit` | R:users I:{id} I:edit |
| GET | `/pages` | R:pages |
| GET | `/pages/new` | R:pages I:new |
| GET | `/pages/{id}/edit` | R:pages I:{id} I:edit |

---

## 5. Account and messaging

| Verb | URL | Segments | Notes |
|------|-----|----------|-------|
| GET | `/account` | G:account | Account hub |
| GET | `/account/{section}` | G:account I:{section} | See §4 hub table |
| GET | `/account/activate` | G:account A:activate | Email activation |
| GET | `/account/verify-email` | G:account R:verify-email | Verification landing |
| GET | `/account/report-signup` | G:account A:report-signup | Abuse report |
| GET | `/account/messages` | G:account R:messages | Mailbox hub panel |
| GET | `/account/messages/{threadId}` | G:account R:messages I:{threadId} | Thread view |
| GET | `/account/messages/compose` | G:account R:messages I:compose | Compose form |
| GET | `/account/messages/blocked` | G:account R:messages I:blocked | Blocked users |
| GET | `/account/messages/components/tab/{tab}` | … I:tab I:{tab} | `open` \| `closed` |

---

## 6. Authentication and password recovery

| Verb | URL | Segments | Notes |
|------|-----|----------|-------|
| GET | `/auth/modal` | G:auth I:modal | Login/signup modal |
| GET | `/password-recovery` | R:password-recovery | Request form page |
| GET | `/password-recovery/reset` | R:password-recovery A:reset | Reset form page |
| POST | `/forms/auth/login` | G:forms G:auth A:login | |
| POST | `/forms/auth/logout` | G:forms G:auth A:logout | |
| POST | `/forms/auth/signup` | G:forms G:auth A:signup | |
| POST | `/forms/auth/password-recovery/request` | G:forms G:auth R:password-recovery A:request | |
| POST | `/forms/auth/password-recovery/reset` | G:forms G:auth R:password-recovery A:reset | |
| POST | `/forms/account/security` | G:forms G:account I:security | Profile/password update |
| POST | `/forms/locale` | G:forms R:locale | Locale switch |

---

## 7. Forms — mutations (`/forms/...`)

### 7.1 Writing and posts

| Verb | URL | Segments |
|------|-----|----------|
| POST | `/forms/write/draft` | G:forms R:write A:draft |
| POST | `/forms/write/publish` | G:forms R:write A:publish |
| GET | `/forms/write/tag-suggestions` | G:forms R:write I:tag-suggestions |
| POST | `/forms/writing/appearance` | G:forms G:writing I:appearance |
| DELETE | `/forms/posts/{postId}` | G:forms R:posts I:{postId} |
| POST | `/forms/posts/{postId}/unpublish` | G:forms R:posts I:{postId} A:unpublish |
| POST | `/forms/posts/{postId}/reading-time` | G:forms R:posts I:{postId} R:reading-time |

### 7.2 Blogs, pages, users

| Verb | URL | Segments |
|------|-----|----------|
| POST | `/forms/blogs` | G:forms R:blogs |
| DELETE | `/forms/blogs/{id}` | G:forms R:blogs I:{id} A:deactivate |
| POST | `/forms/blogs/{blogId}/follow` | G:forms R:blogs I:{blogId} A:follow |
| POST | `/forms/blogs/{blogId}/subscribe` | G:forms R:blogs I:{blogId} A:subscribe |
| POST | `/forms/pages` | G:forms R:pages |
| DELETE | `/forms/pages/{id}` | G:forms R:pages I:{id} |
| POST | `/forms/users` | G:forms R:users |
| POST | `/forms/tags/update` | G:forms R:tags A:update |

### 7.3 Comments

| Verb | URL | Segments |
|------|-----|----------|
| POST | `/forms/posts/{postId}/comments` | G:forms R:posts I:{postId} R:comments |
| POST | `/forms/posts/{postId}/comments/{parentId}/replies` | … I:{parentId} R:replies |
| POST | `/forms/posts/{postId}/comments/{commentId}/approve` | … I:{commentId} A:approve |
| POST | `/forms/posts/{postId}/comments/{commentId}/reject` | … I:{commentId} A:reject |

### 7.4 Highlights and post responses

| Verb | URL | Segments |
|------|-----|----------|
| POST | `/forms/posts/{postId}/highlights` | G:forms R:posts I:{postId} R:highlights |
| DELETE | `/forms/posts/{postId}/highlights/{highlightId}` | … I:{highlightId} |
| POST | `/forms/posts/{postId}/highlight-proposals/{proposalId}/approve` | … R:highlight-proposals I:{proposalId} A:approve |
| POST | `/forms/posts/{postId}/highlight-proposals/{proposalId}/reject` | … A:reject |
| POST | `/forms/highlights/{highlightId}/notes` | G:forms R:highlights I:{highlightId} R:notes |
| GET | `/forms/highlights/{highlightId}/notes/modal` | … I:modal |
| DELETE | `/forms/highlights/{highlightId}/notes/{noteId}` | … I:{noteId} |
| POST | `/forms/highlight-notes/{noteId}/approve` | G:forms R:highlight-notes I:{noteId} A:approve |
| POST | `/forms/highlight-notes/{noteId}/reject` | … A:reject |
| POST | `/forms/post-responses/{responseId}/approve` | G:forms R:post-responses I:{responseId} A:approve |
| POST | `/forms/post-responses/{responseId}/reject` | … A:reject |
| POST | `/forms/post-responses/{responseId}/revoke` | … A:revoke |

### 7.5 Reading list

| Verb | URL | Segments |
|------|-----|----------|
| POST | `/forms/posts/{postId}/reading-list` | G:forms R:posts I:{postId} R:reading-list |
| POST | `/forms/reading-list/{itemId}/read` | G:forms R:reading-list I:{itemId} A:read |
| POST | `/forms/reading-list/{itemId}/unread` | … A:unread |
| DELETE | `/forms/reading-list/{itemId}` | G:forms R:reading-list I:{itemId} |

### 7.6 Notifications

| Verb | URL | Segments |
|------|-----|----------|
| POST | `/forms/notifications/read` | G:forms R:notifications A:read |
| POST | `/forms/notifications/{id}/dismiss` | G:forms R:notifications I:{id} A:dismiss |

### 7.7 Messages

| Verb | URL | Segments |
|------|-----|----------|
| POST | `/forms/messages/compose` | G:forms R:messages I:compose |
| POST | `/forms/messages/blocks/{blockedUserId}` | G:forms R:messages R:blocks I:{blockedUserId} |
| POST | `/forms/messages/threads/{threadId}/close` | G:forms R:messages R:threads I:{threadId} A:close |
| POST | `/forms/messages/threads/{threadId}/flag` | … A:flag |
| POST | `/forms/messages/threads/{threadId}/reply` | … A:reply |
| POST | `/forms/messages/blocks/{blockedUserId}/unblock` | G:forms R:messages R:blocks I:{blockedUserId} A:unblock |

### 7.8 Administration

| Verb | URL | Segments |
|------|-----|----------|
| GET | `/administration/message-reports/{reportId}` | G:administration R:message-reports I:{reportId} |
| POST | `/forms/administration/message-reports/{reportId}/dismiss` | G:forms G:administration R:message-reports I:{reportId} A:dismiss |
| POST | `/forms/administration/message-reports/{reportId}/reviewed` | … A:reviewed |
| POST | `/forms/administration/activitypub` | G:forms G:administration R:activitypub |

### 7.9 ActivityPub (author)

| Verb | URL | Segments |
|------|-----|----------|
| POST | `/forms/writing/activitypub` | G:forms G:writing R:activitypub |
| POST | `/forms/writing/activitypub/follows/{followId}/accept` | … R:follows I:{followId} A:accept |
| POST | `/forms/writing/activitypub/follows/{followId}/reject` | … A:reject |

### 7.10 Images

| Verb | URL | Segments |
|------|-----|----------|
| PUT | `/forms/images/{uuid}/alt` | G:forms R:images I:{uuid} I:alt |

---

## 8. Components — HTMX partials (`/components/...`)

| Verb | URL | Segments | Notes |
|------|-----|----------|-------|
| GET | `/components/menu` | G:components R:menu | Header menu |
| GET | `/components/avatar` | G:components R:avatar | Profile avatar |
| GET | `/components/write-btn` | G:components I:write-btn | Write CTA |
| GET | `/components/seo` | G:components R:seo | Head metadata refresh |
| GET | `/components/notifications/badge` | G:components R:notifications I:badge | Unread count |
| GET | `/components/notifications/overlay` | G:components R:notifications I:overlay | Notification list |
| GET | `/components/blogs/{blogId}/audience` | G:components R:blogs I:{blogId} R:audience | Follow/subscribe widget |
| GET | `/components/images/picker` | G:components R:images I:picker | Image picker shell |
| GET | `/components/images/picker/grid` | … I:grid | Picker grid |
| GET | `/components/confirm-modal/post-delete/{postId}` | G:components I:confirm-modal R:post-delete I:{postId} | |
| GET | `/components/confirm-modal/post-unpublish/{postId}` | … R:post-unpublish I:{postId} | |
| GET | `/components/confirm-modal/message-close/{threadId}` | … R:message-close I:{threadId} | |
| GET | `/components/confirm-modal/message-flag/{threadId}` | … R:message-flag I:{threadId} | |
| GET | `/components/confirm-modal/message-block/{blockedUserId}` | … R:message-block I:{blockedUserId} | |

---

## 9. API (`/api/...`)

| Verb | URL | Segments | Response |
|------|-----|----------|----------|
| POST | `/api/images` | G:api R:images | Upload image |
| GET | `/api/images/{uuid}` | G:api R:images I:{uuid} | Image bytes |
| GET | `/api/images/{filename}` | G:api R:images I:{filename} | Legacy filename lookup |
| DELETE | `/api/images/{uuid}` | G:api R:images I:{uuid} | Delete image |

---

## 10. Internationalization

| Verb | URL | Segments |
|------|-----|----------|
| GET | `/i18n/messages/{locale}.json` | G:i18n R:messages I:{locale} |

---

## 11. ActivityPub federation

| Verb | URL | Segments | Content-Type |
|------|-----|----------|--------------|
| GET | `/.well-known/webfinger` | G:.well-known R:webfinger | JRD JSON |
| GET | `/.well-known/host-meta` | G:.well-known R:host-meta | XRD XML |
| GET | `/{username}/outbox` | I:{username} R:outbox | `activity+json` |
| GET | `/{username}/followers` | I:{username} R:followers | `activity+json` |
| POST | `/{username}/inbox` | I:{username} R:inbox | `activity+json` |
| GET | `/{username}/activities/{activityType}/{activityId}` | I:{username} R:activities I:{activityType} I:{activityId} | `activity+json` |

Actor document: `GET /` on author subdomain with `Accept: application/activity+json` (see [ADR-0008](adr/0008-activitypub-actor-identity.md)).

---

## 12. URL pattern templates

Reusable patterns for new endpoints. Prefer **HTTP verb + resource**; add **Action** only when needed.

### 12.1 Collection read

```
GET /{resource}
GET /{resource}/{identifier}
GET /{resource}/{identifier}/{info}
```

Example: `GET /tags/{slug}`, `GET /tags/{slug}/feed`.

### 12.2 Nested resource

```
GET /{parent-resource}/{parent-id}/{child-resource}
POST /forms/{parent-resource}/{parent-id}/{child-resource}
POST /forms/{parent-resource}/{parent-id}/{child-resource}/{child-id}/{action}
```

Example: `POST /forms/posts/{postId}/comments/{commentId}/approve`.

### 12.3 Hub section

```
GET /{grouping-hub}
GET /{grouping-hub}/{section-identifier}
GET /{grouping-hub}/{section}/components/{info}
```

Example: `GET /writing/library`, `GET /writing/library/components/tab/drafts`.

### 12.4 HTMX partial under public resource

```
GET /{username}/post/{slug}/components/{facet}
GET /{username}/{blogSlug}/post/{slug}/components/{facet}
```

### 12.5 Form mutation

```
POST   /forms/{resource}
POST   /forms/{resource}/{id}/{action}
PUT    /forms/{resource}/{id}/{sub-resource}
DELETE /forms/{resource}/{id}
```

---

## 13. Reserved first segments

Usernames and blog slugs must not collide with application routes. Reserved list: `CustomPagePaths.reservedSegments()` in code — includes `forms`, `api`, `components`, `writing`, `manage`, `account`, `editor`, `administration`, `reading`, `search`, `tags`, `post`, `feed`, `authors`, `explore`, and others.

Post slugs and custom page slugs **may** use reserved words when nested (e.g. `/alice/post/js`).

---

## 14. Maintenance

When adding or changing an `@Path`:

1. Classify each new segment using §1.
2. Add the row to the appropriate §3–§11 table.
3. Update [ARCHITECTURE.md](../ARCHITECTURE.md) if the URL map changes materially.
4. Update [feature-catalog.md](feature-catalog.md) for user-facing navigation.
5. Build canonical links via `*Paths` classes — never hardcode in templates ([contraponto-core.mdc](../.cursor/rules/contraponto-core.mdc)).
