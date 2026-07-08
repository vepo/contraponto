# Feature catalog (UI access)

Living index of **user-facing features reachable through the UI** (header, user menu, modals, in-page links, footer/sidebar). For technical routes, RSS, and APIs see [ARCHITECTURE.md](../ARCHITECTURE.md). For UX narrative see [application-guidelines.md](application-guidelines.md).

**Last verified:** 2026-07-08 · ActivityPub v1.4 multi-blog Fediverse timeline + appearance copy

---

## How to read this document

### Step-counting rules

| Rule | Definition |
|------|------------|
| One step | One deliberate click/tap that opens a new surface or completes sign-in/sign-up (not keystrokes inside a field). |
| Sign in / Sign up | **1 step** (open modal + submit counts as one). |
| Open user menu | **1 step** (avatar + name in header). |
| Menu or nav link | **1 step** each. |
| In-page link or button | **1 step** each. |
| Tab on same page | **+0** if default tab on first load; **+1** if a non-default tab is required. |
| Modal | Opening the modal = **1 step**; typing inside does not add steps. |
| Starting point | Anonymous visitor at **`/`** (featured home), unless the feature naturally starts elsewhere (e.g. Follow on a blog page). |

Paths below list clicks in order. **Steps** = number of steps in that path from the stated starting point.

### Out of scope here

Image JSON API (`/api/images`), email-only flows (`/account/verify-email`, `/account/activate`, password-reset token links). RSS **XML** endpoints are documented in [ARCHITECTURE.md](../ARCHITECTURE.md); this catalog lists **RSS** link buttons in the UI.

---

## Role matrix (user menu)

| Menu section | Guest | `USER` | `EDITOR` | `USER_ADMINISTRATOR` / `ADMIN` |
|--------------|:-----:|:------:|:--------:|:------------------------------:|
| Header: Search, Sign In/Up | yes | — | — | — |
| Header: Write, Publish, Save draft | — | on `/write` only | on `/write` only | on `/write` only |
| Header: Notifications bell | — | yes | yes | yes |
| My Blog, Writing, Reading, Manage, Account | — | yes | yes | yes |
| Review hub (editor) | — | — | yes | yes |
| Administration hub | — | — | — | yes |

---

## Public reading (guest or any visitor)

| Feature | Audience | URL | Steps | UI path (from `/`) |
|---------|----------|-----|------:|---------------------|
| Featured homepage | anyone | `GET /` | 0 | Land on `/` (or click logo). Guests see an optional dismissible editorial masthead above the featured hero. |
| Author directory | anyone | `GET /authors` | 1 | On `/` → **Autores** in home right margin (wide screens). |
| Author profile | anyone | `GET /authors/{username}` | 2 | Home → **Autores** card → author card. |
| Blog directory | anyone | `GET /explore/blogs` | 1 | On `/` → **Blogs** in home right margin (wide screens). |
| Featured homepage (return) | anyone | `GET /` | 1 | Click **contraponto** logo in header. |
| Load more featured posts | anyone | `GET /components/home/grid?page=` | 1 | On `/` → **Load more**. |
| Blog home (default blog) | anyone | `GET /{username}` | 2 | Home → open a post author byline **or** search result **or** **My Blog** (signed in). |
| Multi-blog profile | anyone | `GET /{username}` | 2 | Home → author with multiple blogs (lists blogs instead of redirecting). |
| Blog home (secondary blog) | anyone | `GET /{username}/{blogSlug}` | 3 | Home → multi-blog profile → blog name. |
| Read post (main blog) | anyone | `GET /{username}/post/{slug}` | 3 | Home → blog → post card. |
| Read post (secondary blog) | anyone | `GET /{username}/{blogSlug}/post/{slug}` | 4 | Home → profile → secondary blog → post. |
| Serie listing | anyone | `GET /{username}/serie/{slug}` or `…/{blogSlug}/serie/{slug}` | 3–4 | Open post in serie → serie title link in serie nav. |
| Tag listing | anyone | `GET /tags/{slug}` | 3 | Open post → click tag chip (shows **Principais autores** when applicable). |
| Custom page (global) | anyone | `GET /page/{slug}` | 2 | Home → footer/sidebar custom page link. |
| Custom page (user/blog) | anyone | `GET /{username}/page/{slug}` etc. | 2–3 | Footer/sidebar link (depends on placement). |
| Sidebar navigation | anyone | varies | 2 | Home → header **menu** icon (left) → sidebar link. |
| Quick search | anyone | `GET /search/modal` | 1 | Header **search** icon. |
| Quick search → open result | anyone | post/blog URL | 2 | Search icon → click a result. |
| Full search page | anyone | `GET /search` | — | **No primary UI link** in header/menu (UI-only catalog omits direct-URL access). |
| Load more (blog/tag lists) | anyone | HTMX grid fragment | +1 | On listing page → **Load more**. |
| Site RSS feed link | anyone | `GET /feed` | 1 | Home → **RSS** above grid **or** footer **RSS**. |
| Blog RSS feed link | anyone | `GET /{username}/feed/main-blog` or `…/{blogSlug}/feed` | 2–3 | On blog home → **RSS** in header actions. |
| Tag RSS feed link | anyone | `GET /tags/{slug}/feed` | 3 | On tag page → **RSS** in header actions. |
| Serie RSS feed link | anyone | `…/serie/{slug}/feed` | 3–4 | On serie page → **RSS** in header actions. |

---

## Authentication

| Feature | Audience | URL | Steps | UI path (from `/`) |
|---------|----------|-----|------:|---------------------|
| Sign up | guest | `GET /auth/modal?mode=signup` | 1 | Header → **Sign Up** → submit → check email to activate (no session until activation link). |
| Sign in | guest | `GET /auth/modal?mode=login` | 1 | Header → **Sign In**. |
| Account activation (email link) | guest (inactive account) | `GET /account/activate?token=…` | — | Link in signup email; activates account and starts session (email-only). |
| Language preference | anyone | `POST /forms/locale` | 2 | Header or footer **flag** → pick language (also list on Account hub). |
| Password recovery request | guest | `GET /password-recovery` | 2 | **Sign In** modal → **Forgot password?** |
| Sign out | signed in | `POST /forms/auth/logout` | 2 | Open user menu → **Sign out**. |

---

## Writing (`USER`, blog owner)

| Feature | Audience | URL | Steps | UI path (from `/`) |
|---------|----------|-----|------:|---------------------|
| Writing hub (library) | `USER` | `GET /writing` | 2 | Open user menu → **Writing** (library panel default). |
| Reading hub (highlights) | `USER` | `GET /reading` | 2 | Open user menu → **Reading** (highlights panel default). |
| Reading hub — saved for later | `USER` | `GET /reading/saved` | 2 | Open user menu → **Reading** → **Saved for later** in left nav. |
| Reading hub — notes | `USER` | `GET /reading/notes` | 2 | Open user menu → **Reading** → **Notes** in left nav. |
| New post (from hub) | `USER` | `GET /write` | 3 | Open user menu → **Writing** → **Write** in left nav. |
| New post | `USER` | `GET /write` | 1 | Header **Escrever** (Write) button. |
| Edit draft/post | `USER` | `GET /write/draft/{id}` | 3 | Open user menu → **Writing** → **Edit** on library row. |
| Edit own published post | `USER` | `GET /write/draft/{id}` | 4 | Home → blog → post → **Edit** (author only). |
| Library (published tab) | `USER` | `GET /writing/library` + tab | 2 | **Writing** hub → **Published** tab (+0). |
| Unpublish post | `USER` | `POST /forms/posts/{postId}/unpublish` | 3 | **Writing** hub → **Published** tab → **Despublicar** on row. |
| Delete draft | `USER` | `DELETE /forms/posts/{postId}` | 3 | **Writing** hub → **Drafts** tab → **Excluir** on row. |
| Save draft | `USER` | `POST /forms/write/draft` | — | On `/write` → header **Salvar Rascunho** (no extra navigation). |
| Publish post | `USER` | `POST /forms/write/publish` | — | On `/write` → header **Publicar**. |
| Image library (Writing hub) | `USER` | `GET /writing/images` (+ optional `?q=` search) | 2 | Open user menu → **Writing** → **Images** in left nav; search filters by alt text or path. |

---

## Manage own content (`USER`, blog owner)

| Feature | Audience | URL | Steps | UI path (from `/`) |
|---------|----------|-----|------:|---------------------|
| Manage hub (dashboard) | `USER` | `GET /manage` | 2 | Open user menu → **Manage** (dashboard panel default). |
| Dashboard analytics | `USER` | `GET /manage/dashboard/components/analytics` | 2 | **Manage** hub (month controls on same panel). |
| My Blog shortcut | `USER` | `GET /{username}` | 2 | Open user menu → **My Blog**. |
| Blog list (author) | `USER` | `GET /writing/blogs` | 2 | Open user menu → **Writing** → **Blogs** in left nav. |
| Blog list (platform) | `EDITOR` | `GET /manage/blogs` | 2 | Open user menu → **Manage** → **Blogs** in left nav. |
| New blog | `USER` | `GET /blogs/new` | 3 | **Writing** → **Blogs** → **New Blog**. |
| Edit blog (core) | `USER` | `GET /blogs/{id}/edit?hub=writing` | 4 | **Writing** → **Blogs** → **Edit** on row. |
| Blog settings (extended) | `USER` | `GET /blogs/{id}/settings` | 4 | **Writing** → **Blogs** → **Settings** on row. |
| Blog image library (legacy redirect) | `USER` | `GET /blogs/{blogId}/images` → `/writing/images` | 2 | Bookmarked blog URL redirects to Writing hub **Images**. |
| Git sync history | `USER` | `GET /blogs/{blogId}/git-sync` | 5 | **Blogs** → **Edit** → **View sync history**. |
| Git sync run detail | `USER` | `GET /blogs/{blogId}/git-sync/{runId}` | 6 | Sync history → run link. |
| Custom pages list | `USER` | `GET /manage/pages` | 2 | Open user menu → **Manage** → **Custom Pages** in left nav. |
| New custom page | `USER` | `GET /pages/new` | 4 | **Custom Pages** → **New Page**. |
| Edit custom page | `USER` | `GET /pages/{id}/edit` | 4 | **Custom Pages** → **Edit** on row. |
| Comment moderation inbox | `USER` | `GET /manage/comments` | 2 | Open user menu → **Manage** → **Comments** in left nav. |
| Account hub (notifications) | `USER` | `GET /account` | 2 | Open user menu → **Account** (notifications panel default). |
| Account security | `USER` | `GET /account/security` | 2 | Open user menu → **Account** → **Security** in left nav. |
| Author appearance | `USER` | `GET /writing/appearance` | 2 | Open user menu → **Writing** → **Appearance** in left nav. |
| Fediverse publishing settings | `USER` | `POST /forms/writing/activitypub` | 2 | Open user menu → **Writing** → **Appearance** → toggle **Publish to the Fediverse** (all blogs). |
| Fediverse follow request moderation | `USER` | `POST /forms/writing/activitypub/follows/{followId}/accept` or `/reject` | 2 | Open user menu → **Writing** → **Appearance** → **Accept/Reject** on pending Fediverse follow requests. |
| Notifications overlay | `USER` | `GET /components/notifications/overlay` | 1 | Header bell → dropdown preview (stays on current page). |
| Notifications inbox | `USER` | `GET /account/notifications` | 2 | Header bell → **View all notifications** **or** user menu → **Account**. |
| Notifications (menu path) | `USER` | `GET /account` | 2 | Open user menu → **Account**. |
| Subscriptions | `USER` | `GET /account/subscriptions` | 2 | Open user menu → **Account** → **Subscriptions** in left nav. |
| Messages mailbox | `USER` | `GET /account/messages` | 2 | Open user menu → **Account** → **Messages** in left nav. |
| Compose message | `USER` | `GET /account/messages/compose` | 3 | **Messages** → **Compose** (or author profile → **Message**). |
| Message thread | `USER` | `GET /account/messages/{threadId}` | 3 | **Messages** → open row. |
| Blocked users | `USER` | `GET /account/messages/blocked` | 2 | Open user menu → **Account** → **Blocked users** in left nav. |
| Message author (profile) | signed in | `GET /account/messages/compose?to={username}` | 3 | Author profile → **Message**. |

---

## Social (reader / author)

| Feature | Audience | URL | Steps | UI path |
|---------|----------|-----|------:|---------|
| Follow blog | signed in | `POST /forms/blogs/{blogId}/follow` | 2 | Blog or post page → **Follow** (starts on that blog/post). |
| Follow blog (guest) | guest | via login modal | 3 | Blog/post → **Follow** → **Sign in** (1) + complete login. |
| Email subscribe | signed in | `POST /forms/blogs/{blogId}/subscribe` | 2 | Blog/post → **Subscribe by email**. |
| Email subscribe (guest) | guest | via login modal | 3 | Blog/post → **Subscribe by email** → **Sign in**. |
| Post comment | signed in | `POST /forms/posts/{postId}/comments` | 3 | Home → blog → post → submit comment form. |
| Post comment (guest) | guest | via login modal | 4 | Post → **Sign in** → submit comment. |
| Save post for later | signed in | `POST /forms/posts/{postId}/reading-list` | 3 | Home → blog → post → **Save for later** in page-top actions. |
| Share post | guest, signed in | — (client + external) | 3 | Home → blog → post → **LinkedIn**, **Bluesky**, or **Copy** in action bar. |
| Share blog | guest, signed in | — (client + external) | 2 | Blog home → **LinkedIn**, **Bluesky**, or **Copy** in header actions. |
| Mark post as read (reading list) | signed in | `POST /forms/reading-list/{itemId}/read` | 3 | Post **Mark as read** **or** Reading hub → Saved → row action. |
| Highlight passage | signed in | `POST /forms/posts/{postId}/highlights` | 3 | Home → blog → post → select text in body → **Highlight**. |
| Highlight passage (guest) | guest | via login modal | 4 | Post → select text → **Sign in to highlight**. |
| Add note to highlight | signed in | `GET /forms/highlights/{id}/notes/modal`, `POST …/notes` | 4 | Post → select text → **Add note** → dialog → **OK**. |
| Remove highlight | signed in | `DELETE /forms/posts/{postId}/highlights/{id}` | 4 | Post → click owned highlight mark → **Remove highlight**. |
| Remove note | signed in | `DELETE /forms/highlights/{id}/notes/{noteId}` | 4 | Post → click owned note card → **Remove note**. |
| My highlights library | signed in | `GET /reading/highlights` | 2 | Open user menu → **Reading** **or** post highlights section → **My highlights**. |
| Reply to comment | signed in | `POST …/comments/{parentId}/replies` | 4 | Post → **Reply** on comment → submit. |
| Version history modal | author | `GET …/components/history/modal` | 4 | Home → own published post → **Version N** control. |
| Approve/reject comment (on post) | post owner | `POST /forms/posts/…/comments/…` | — | Post page pending section (author viewing own post). |

---

## Editor (`EDITOR`)

| Feature | Audience | URL | Steps | UI path (from `/`, signed in) |
|---------|----------|-----|------:|-------------------------------|
| Review hub (featured posts) | `EDITOR` | `GET /editor` | 2 | Open user menu → **Review** (featured posts panel default). |
| Featured review list | `EDITOR` | `GET /editor/review` | 2 | Same as Review hub (bookmarkable section URL). |
| Toggle featured (review) | `EDITOR` | `PUT /editor/review/components/{postId}/featured/toggle` | 4 | **Featured Posts** → star on row. |
| Toggle featured (on post) | `EDITOR` | `PUT …/component/featured/toggle` | 3–4 | Open post → star control in action bar. |
| Tag admin list | `EDITOR` | `GET /editor/tags` | 2 | Open user menu → **Review** → **Tags** in left nav. |
| Edit tag metadata | `EDITOR` | `GET /tags/{slug}/edit` | 4 | **Tags** → **Edit** on row. |
| Edit tag (from public tag page) | `EDITOR` | `GET /tags/{slug}/edit` | 4 | Home → post → tag → **Edit** (editor-only link on tag page). |

---

## User administration (`USER_ADMINISTRATOR`, `ADMIN`)

| Feature | Audience | URL | Steps | UI path (from `/`, signed in) |
|---------|----------|-----|------:|-------------------------------|
| Administration hub (users) | admin | `GET /administration` | 2 | Open user menu → **Administration** (users panel default). |
| User list | admin | `GET /administration/users` | 2 | Same as Administration hub (bookmarkable section URL). |
| Platform insights | admin | `GET /administration/insights` | 3 | Open user menu → **Administration** → **Platform insights**. |
| ActivityPub global kill-switch | admin | `GET /administration/activitypub`, `POST /forms/administration/activitypub` | 3 | Open user menu → **Administration** → **Fediverse** → toggle **Enable ActivityPub federation globally**. |
| Message reports | `ADMIN` | `GET /administration/message-reports` | 3 | Open user menu → **Administration** → **Message reports**. |
| New user | admin | `GET /users/new` | 4 | **Users** → **New User**. |
| Edit user | admin | `GET /users/{id}/edit` | 4 | **Users** → **Edit** on row. |

---

## Dev personas (manual testing)

Use [dev-import.sql](../src/main/resources/dev-import.sql) — all dev users share the **admin** password.

| Username | Roles | Use to reach |
|----------|-------|----------------|
| `alice`, `bob`, `carol` | `USER` | Write, Library, Blogs, Dashboard, own posts |
| `alice` | `USER` | Secondary blog `lab-notes` (Fediverse multi-blog Creates / outbox); main posts + ActivityPub actor |
| `bob` | `USER` | Secondary blog `architecture-notes`, multi-blog URLs |
| `dave` | `USER` | Follower, notifications |
| `eve` | `USER` | Email subscriber |
| `editor` | `USER`, `EDITOR` | `/review`, `/tags/manage`, featured toggle on posts |
| `admin` | `ADMIN`, `USER_ADMINISTRATOR` | `/users`, platform admin |

Run `./mvnw quarkus:dev` → [http://localhost:8080](http://localhost:8080).

---

## Navigation map (authenticated menu)

```mermaid
flowchart TD
  subgraph header [Header]
    Home[Logo home]
    Search[Search modal]
    WriteBtn[Write button]
    Badge[Notifications bell]
    Menu[User menu]
  end
  Menu --> MyBlog["My Blog"]
  Menu --> WritingHub["/writing"]
  Menu --> ManageHub["/manage"]
  Menu --> AccountHub["/account"]
  Menu --> EditorHub["/editor"]
  Menu --> AdminHub["/administration"]
  WritingHub --> Write["/write"]
  WritingHub --> Library["/writing/library"]
  ManageHub --> Dashboard["/manage/dashboard"]
  ManageHub --> Blogs["/manage/blogs"]
  ManageHub --> Pages["/manage/pages"]
  ManageHub --> Comments["/manage/comments"]
  AccountHub --> Notifications["/account/notifications"]
  AccountHub --> Security["/account/security"]
  WritingHub --> Appearance["/writing/appearance"]
  WritingHub --> AuthorBlogs["/writing/blogs"]
  EditorHub --> Review["/editor/review"]
  EditorHub --> TagsManage["/editor/tags"]
  AdminHub --> Users["/administration/users"]
  Blogs --> BlogEdit["/blogs/id/edit"]
  BlogEdit --> Images["/blogs/id/images"]
  BlogEdit --> GitSync["/blogs/id/git-sync"]
```

Full-page surfaces show a **breadcrumb trail** (Home or hub root → current page). Breadcrumbs add zero navigation steps.

**Sources:** [MenuEndpoint/menu.html](../src/main/resources/templates/MenuEndpoint/menu.html), [components/header.html](../src/main/resources/templates/components/header.html), [ARCHITECTURE.md](../ARCHITECTURE.md).
