# Contraponto — Domain Specification

Canonical domain language for Contraponto, a multi-blog publishing platform. Developers, reviewers, and AI agents must align code, tests, and UI copy with this document.

**Related references:** [ARCHITECTURE.md](../ARCHITECTURE.md) (technical patterns), [application-guidelines.md](application-guidelines.md) (routes and flows).

**Maintenance:** When a change introduces or alters domain concepts, UI labels, or business rules, update this file **before** merging (see [.cursor/rules/domain-model.mdc](../.cursor/rules/domain-model.mdc)).

---

## Context

Contraponto lets **authors** publish **posts** on one or more **blogs**, curators (**editors**) feature content on the **home page**, and **readers** discover posts, follow blogs, subscribe by email, and comment. Content is server-rendered (Qute + HTMX); the domain model lives in `dev.vepo.contraponto.*`.

```mermaid
erDiagram
    User ||--o{ Blog : owns
    Blog ||--o{ Post : contains
    Blog ||--o{ Serie : groups
    Post }o--o{ Tag : tagged_with
    Post ||--o{ PostPublication : versioned_by
    Post ||--o| PostPublication : live_publication
    Post ||--o{ PostComment : receives
    User ||--o{ BlogAudience : audience_member
    Blog ||--o{ BlogAudience : has_audience
    User ||--o{ Notification : receives
    Blog ||--o{ CustomPage : may_have
    User }o--o| Image : profile_picture
    User }o--o| Image : default_blog_banner
    Blog }o--o| Image : banner
```

---

## Ubiquitous Language

Terms below are the **only** approved names for aggregates, entities, value objects, states, actions, and user-visible labels unless this document is updated first.

### Platform & people

| Term | Meaning | Code / notes |
|------|---------|--------------|
| **Contraponto** | The publishing platform (product). | — |
| **Guest** | Unauthenticated visitor; may read public content. | No session |
| **User** | Registered account (`tb_users`): username, email, display name, password, roles, active flag; optional **profile picture** and **default blog banner**. | `User` |
| **Author** | User who owns at least one blog and writes posts. Implied by blog ownership, not a separate role. | `Post.getAuthor()` → blog owner |
| **Reader** | Any user or guest consuming published content. | — |
| **Session** | Authenticated browser state (`__session` cookie → `SessionStore` → user id, then `User` from DB). | `LoggedUser`, `SessionStore` |
| **Session store** | Backing store for login sessions: in-memory (single instance) or Redis (multi-instance). | `SessionStore`, `SessionStoreProducer` |
| **Password recovery** | Self-service flow to reset a forgotten password via email link. | `PasswordRecoveryEndpoint` |
| **Password reset token** | Single-use, time-limited secret sent by email; stored hashed in `tb_user_account_tokens`. | `UserAccountToken` |
| **Pending email** | New email address awaiting verification; login and notifications use the confirmed email until verified. | `User.pendingEmail` |
| **Email verification** | Confirms a **pending email** via link; promotes it to the account email. | `EmailVerificationEndpoint` |
| **Account email** | Transactional HTML message for access or user management (password reset, password changed, email verification). | `AccountEmailService` |
| **Role** | Platform capability assigned to a user (multi-role). | `Role` enum |
| **User** (role) | Default role; write own content. | `Role.USER` — label: "User" |
| **Editor** | Curate site-wide: feature posts, review queue, tag metadata. | `Role.EDITOR` — label: "Editor" |
| **User administrator** | Manage users and roles (except assigning Administrator). | `Role.USER_ADMINISTRATOR` |
| **Administrator** | Full access including assigning Administrator. | `Role.ADMIN` |

### Blogs

| Term | Meaning | Code / notes |
|------|---------|--------------|
| **Blog** | A publication channel owned by exactly one user. Has name, slug (unique per owner), description, optional **blog banner**, active flag. | `Blog` |
| **Blog description** | Optional short bio on **blog home**; authors may use **Markdown** (bold, links, lists). Rendered to safe HTML on the public blog home only; does not support **content render plugins**. | `Blog.description`, `BlogDescriptionRenderer` |
| **Main blog** | The blog auto-created for a user (`main = true`); slug typically matches username. | `Blog.main` |
| **Secondary blog** | Additional blog owned by the same user (`main = false`). | `Blog` |
| **Blog owner** | User who owns the blog; sole writer for that blog's posts and the only role that may edit blog settings. | `Blog.owner` |
| **Profile picture** | Optional image on the user; shown in the menu and wherever the author is displayed. When absent, a **generated avatar** shows the user's initials on a brand-colored SVG (`GET /components/avatar`). | `User.profilePicture`, `AvatarEndpoint` |
| **Default blog banner** | Optional image on the user used when a blog has no own **blog banner**. | `User.defaultBlogBanner` |
| **Blog banner** | Optional hero image on a blog; overrides the owner's default for that blog's public home. | `Blog.banner` |
| **Effective blog banner** | `blog.banner` if set, else `user.defaultBlogBanner`. | `BlogBannerService.resolveEffectiveBanner` |
| **Active blog** | Blog with `active = true`; inactive blogs return 404 on public routes. | `Blog.active` |
| **Blog home** | Public listing of published posts for one blog. | `BlogEndpoint` |
| **User profile** | Public page at `/{username}` when the user has multiple blogs: lists their blogs. | `BlogEndpoint` |

### Posts & publishing

| Term | Meaning | Code / notes |
|------|---------|--------------|
| **Post** | A piece of content belonging to one blog: title, slug (unique per author), description, body, format, cover, tags, optional serie, published flag, featured flag, timestamps. Working copy for the author. | `Post` |
| **Post description** | Optional short excerpt on the working copy (`Post.description`); capped at **512 characters** in storage. Shown on post cards and metadata when published. | `Post.description`, `PostPublicationDescriptions` |
| **Published snapshot description** | Excerpt copied into each `PostPublication` when publishing; same **512-character** cap as the working copy. Git import truncates longer Jekyll excerpts and may log a sync warning. | `PostPublication.description` |
| **Draft** | Post with `published = false`; visible only to the author (and editors where applicable). | Library tab: "Drafts" |
| **Published post** | Post with `published = true`; visible to readers (subject to blog active). | Library tab: "Published" |
| **Slug** | URL-safe identifier for a post, blog, tag, serie, or custom page. | Field on entities |
| **Cover** | Optional hero image for a post (`Image`). | `Post.cover` |
| **Format** | Markup dialect: **Markdown** or **AsciiDoc**. | `Format` enum |
| **Content render plugin** | Pluggable handler registered via `ServiceLoader` that turns a **render tag** in post body into HTML (e.g. YouTube embed). | `ContentRenderPlugin` |
| **Render tag** | Author syntax in post body: `{% renderIdentifier param1 param2 %}`. Built-in identifiers: `youtube`, `gist`, `github`, `twitter`. Unknown identifiers remain literal in output. | `ContentRenderTagProcessor` |
| **Publish** | Action that marks the post published, creates a **publication snapshot**, sets **live publication**, fires `PostPublishedEvent`, and may trigger Git export and notifications. | `PostPublicationService.publish` |
| **Republish** | Publish again when content differs from live snapshot; increments version, re-notifies audience. | Same service |
| **Publication snapshot** | Immutable `PostPublication` row: version, content/tags/cover at publish time. | `PostPublication` |
| **Live publication** | The snapshot readers see; pointer on `Post.livePublication`. | `Post.livePublication` |
| **Unpublished changes** | Working post differs from live snapshot while still published. | `hasUnpublishedChanges` |
| **Featured** | Post flagged for homepage curation (`featured = true`). | Editor toggle |
| **Version history** | Diff of publication snapshots; metadata shows live version; full list in a modal on the post page (all readers). | `PostChangeDiffService` |
| **Serie** | Ordered collection of posts within one blog. | `Serie` |
| **Tag** | Global taxonomy label; posts link via join table; snapshots copy tags at publish. | `Tag` |
| **Uploaded image** | Blog-scoped image: metadata in `tb_images`, bytes in `tb_image_content` (PostgreSQL `BYTEA`), optional **alt text**, optional **Git asset path** (relative path under the blog's assets directory, without extension, set on Git import). Served at `/api/images/{uuid}{ext}` regardless of Git path. | `Image`, `ImageContent` |
| **Git asset path** | Original relative path of an image file in the Git/Jekyll assets tree (e.g. `capas/photo`). Used for Git export filenames and Images hub display; Contraponto content still references `/api/images/{uuid}.ext`. | `Image.gitAssetRelativePath` |
| **Image marker** | HTML comment in stored body: `<!-- contraponto:image uuid="…" -->` immediately before an image reference; hidden in the Write editor and stripped when rendering published post content (markers remain in stored content). | `ContentImageMarkerService` |
| **Image lightbox** | Reader expands an inline post-body image in an on-page overlay (larger view, same image URL); caption prefers AsciiDoc **image block title**, then **alt text**; omits caption when only a filename is available. Closed with ESC, close control, or backdrop click. | `#image-lightbox`, `ImageLightboxManager` in `main.js` |
| **Code block copy** | Reader copies the plain source from a fenced or listing code block via a **Copy** control; label briefly shows **Copied**. | `CodeCopyManager` in `main.js`, `.code-block__copy` |
| **Image dependency** | Record that a post, publication snapshot, or custom page uses an uploaded image (`INLINE` or `COVER`). | `PostImageDependency`, `CustomPageImageDependency` |
| **Image control** | Manage screen listing a blog's uploaded images, where each is used, and alt text editing. | `ImageControlEndpoint` |

### Custom pages

| Term | Meaning | Code / notes |
|------|---------|--------------|
| **Custom page** | Static HTML/Markdown page: title, slug, section, content, placement, published flag; optional blog scope. | `CustomPage` |
| **Global custom page** | Page not tied to a blog (`blog = null`); URL `GET /page/{slug}`. | `PageType.GLOBAL` |
| **Blog custom page** | Page scoped to a blog; URL includes owner username and `/page/` segment. | `CustomPagePaths.publicUrl` |
| **Page placement** | Where navigation surfaces the page: **Footer**, **Sidebar**, or **None** (direct URL only). | `PagePlacement` |
| **Custom page cache** | In-memory published pages read by `CustomPageEndpoint`; invalidated on `CustomPageChangedEvent`. | `CustomPageCache` |
| **RSS feed cache** | Caffeine cache of rendered RSS XML; invalidated on `PostPublishedEvent`. | `RssFeedService`, `rss-feeds` |

### Audience & notifications

| Term | Meaning | Code / notes |
|------|---------|--------------|
| **Blog audience** | Per user–blog relationship: follow and/or email subscribe flags. | `BlogAudience` |
| **Follow** | In-app notifications when the blog publishes (`followed = true`). | `BlogAudienceFollowEndpoint` |
| **Following** | Active follow state (button label when on). | UI: "Following" |
| **Subscribe by email** | Email on publish (`emailSubscribed = true`); deduped via email notification log. | `BlogAudienceSubscribeEndpoint` |
| **Subscribed** | Active email subscription (button label when on). | UI: "Subscribed" |
| **Notification** | In-app item for a recipient: type, blog, optional post/publication/actor/comment, read flag. | `Notification` |
| **Notification bell** | Header control showing unread count; opens **notification overlay**. | `NotificationBadgeEndpoint`, `#notificationBellBtn` |
| **Notification overlay** | Dropdown preview of latest unread notifications. | `NotificationOverlayEndpoint`, `#notificationOverlay` |
| **Dismiss notification** | Mark one notification read from the overlay. | `DismissNotificationEndpoint` — button: "Dismiss" |
| **Close notification overlay** | Hide the dropdown without changing read state. | Close control (`data-notification-close`) |
| **Notifications changed** | HTMX event; refreshes badge and open overlay. | `HtmxTriggers.NOTIFICATIONS_CHANGED_ON_BODY` |
| **Notification type** | `NEW_POST`, `NEW_FOLLOW`, `NEW_SUBSCRIBE`, `NEW_COMMENT`. | `NotificationType` |
| **Follow after login** | Guest clicks Follow, signs in via modal, then clicks Follow again. | Post page / audience widget |
| **Subscriptions page** | Authenticated list of blogs the user follows/subscribes to. | `SubscriptionEndpoint` |

### Comments

| Term | Meaning | Code / notes |
|------|---------|--------------|
| **Post comment** | Text reply on a published post; may be root or nested under an approved parent. | `PostComment` |
| **Comment body** | Required text, max 2000 characters after trim. | `MAX_BODY_LENGTH` |
| **Comment status** | **Pending**, **Approved**, or **Rejected**. | `CommentStatus` |
| **Moderation** | Post owner approves or rejects pending comments. | `PostCommentService` |
| **Root comment** | Top-level comment (`parent = null`). | `PostComment.isRoot` |
| **Reply** | Comment whose parent must be **Approved**. | `createReply` |

### Git sync

| Term | Meaning | Code / notes |
|------|---------|--------------|
| **Git integration** | Per-blog export/import to a remote Git repo over HTTPS (any host; Jekyll layout). Available on the **main blog** and **secondary blogs**; configured on blog **Edit** (default blog) or **Settings** (`GET /blogs/{id}/settings`). | `Blog.gitEnabled`, etc. |
| **Git sync request** | Event after draft save or publish to export post to Git when enabled. | `PostGitSyncRequestedEvent` |
| **Remote poll** | Scheduled pull of remote changes when poll enabled. | `GitRemotePollScheduler` |
| **Git sync run** | One execution of Git export (push) or import (pull) for a blog. | `GitSyncRun` |
| **Git sync trigger** | What started the run: draft save, publish, remote poll, or blog save warmup. | `GitSyncTrigger` |
| **Git sync operation** | `EXPORT` (Contraponto → remote) or `IMPORT` (remote → Contraponto). | `GitSyncOperation` |
| **Git sync outcome** | `SUCCESS`, `PARTIAL`, `FAILED`, or `SKIPPED`. | `GitSyncOutcome` |
| **Git error kind** | Classification of failure: `NONE`, `AUTHENTICATION`, `NETWORK`, `REPOSITORY`, `WORKSPACE`, `CONVENTION`, `POST`, `UNKNOWN`. | `GitErrorKind` |
| **Repository readable** | Contraponto prepared the workspace and resolved layout (`_contraponto.yml` or defaults). | Flag on `GitSyncRun` |
| **Data loadable** | Remote was reachable and clone/fetch/pull succeeded. | Flag on `GitSyncRun` |
| **Git sync log entry** | One step or per-post result within a run (phase, message, remediation). | `GitSyncRunEntry` |
| **Legacy Jekyll front matter** | Import-only YAML aliases (`permalink`, `image`, `publish_date`, `series`) mapped to slug, cover, publish time, and serie; native keys win when both are set. | `GitFrontMatterResolver` |

### Discovery & feeds

| Term | Meaning | Code / notes |
|------|---------|--------------|
| **Home page** | Site landing with **featured** published posts (hero + grid). | `HomeEndpoint` |
| **Search** | Full-text discovery via modal or `/search` page. | `SearchEndpoint` |
| **Tag page** | Public listing of posts with a given tag. | `TagPageEndpoint` |
| **RSS feed** | Syndication for site, blog, serie, or tag. | `rss` package |
| **RSS feed link** | Public control that opens the matching feed URL in a new tab. | `components/rss-feed-link.html`, `RssFeedPaths` |
| **View count** | Read metric per post load (one row per page GET per session). | `View` |
| **Estimated read time** | Word-count hint on post cards (e.g. "5 min read"); not tracked engagement. | `TemplateExtensions.readTime` |
| **Reading time** | Actual seconds a reader spends on a published post while the browser tab is **visible**; extended by 5-second client heartbeats. | `ReadingSession` |
| **Reading session** | One post + `__view_session` (+ optional user) row accumulating `total_seconds`. | `ReadingSession` |
| **Average reading time** | Mean `total_seconds` across reading sessions for a post; shown on post metadata. | `ReadingTimeRepository.averageSecondsByPost` |

### Author workspaces

| Term | Meaning | Code / notes |
|------|---------|--------------|
| **Write** | Editor for creating or editing a post (`/write`, `/write/draft/{id}`). | `WriteEndpoint` |
| **Image control** | Per-blog list of uploaded images, usages, and alt text. Reachable from Writing hub **Images** (`/writing/images`) with a blog switcher, or directly at `/blogs/{id}/images`. | `ImageControlEndpoint`, Writing hub `images` section |
| **Library** | Author's drafts and published posts across owned blogs. | `LibraryEndpoint` |
| **Dashboard** | Author overview per selected blog: analytics (daily views, daily reading time, new followers, new email subscribers by month), counts, and recent drafts/published. | `DashboardEndpoint` |
| **Dashboard analytics** | Time-series metrics for one blog: daily views (with optional comparison to the previous calendar month), daily reading time, daily new follows, daily new email subscribes. | `DashboardAnalyticsService` |
| **Account security** | Update email (with verification) and password. | `AccountSecurityEndpoint`, `AccountSecurityUpdateEndpoint` |
| **Author appearance** | Update display name, profile picture, and default blog banner. | `AuthorAppearanceEndpoint`, `AuthorAppearanceUpdateEndpoint` |
| **Author blogs** | List, create, and edit own blogs (name, slug, banner) in the Writing hub. Extended settings (description, active, Git) on the blog settings form. | `BlogManageEndpoint`, Writing hub `blogs` section |
| **Platform blog management** | Editors list all blogs and deactivate others’ secondary blogs. | `BlogManageEndpoint`, Manage hub `blogs` section (`EDITOR`+) |
| **User management** | Administrators create and edit users, roles, and passwords. | `UserManageEndpoint`, `UserSaveEndpoint` |
| **Review** | Editor queue of published posts to toggle featured. | `ReviewEndpoint` — title: "Review Featured Posts" |
| **Navigation hub** | Logged-in shell with sticky left sidebar sections and distinct URLs per feature (Writing → Library, Images, Blogs, Appearance; Manage; Account; Review; Administration). Writing hub does not duplicate the header Write action. Manage **Blogs** nav is visible only to `EDITOR`+. Menu opens the hub default section. | `navigation` package — `/writing`, `/manage`, `/account`, `/editor`, `/administration` and `/{hub}/{section}` |
| **Breadcrumb trail** | Ordered navigation labels from Home or a hub to the current page; last item is not linked. | `BreadcrumbService`, `components/breadcrumb.html` |
| **Locale** | User-facing language for interface chrome (`pt-BR`, `en`, `es`). Default is **pt-BR** (text in HTML). | `LocalePreference`, cookie `contraponto_locale` |
| **Language preference** | Persisted locale choice; applied client-side via `data-i18n` markers. | `LocaleSwitchEndpoint`, `i18n.js` |
| **Language switcher** | Compact PT / EN / ES control in header and footer; full control with hint on Account hub. | `components/locale-switcher.html` |

### Interface internationalization (i18n)

- **Default locale:** `pt-BR` — canonical copy lives in Qute templates (visible without JavaScript).
- **Secondary locales:** `en`, `es` — JSON bundles at `GET /i18n/messages/{locale}.json`; the browser applies them to elements with `data-i18n` keys.
- **Scope:** menus, forms, validation messages, toasts, pagination, hub chrome — **not** post/comment/blog body, custom page content from DB, or account emails.
- **Keys:** dot-separated identifiers (e.g. `auth.signIn`, `menu.writing`). Full catalog: `src/main/resources/i18n/messages_en.json` and `messages_es.json`.

### UI labels (user-visible copy)

Templates use **PT-BR** as default text with `data-i18n` keys. English and Spanish are in the JSON bundles. Email subjects below are **not** translated by the interface i18n layer.

| UI element | i18n key | PT-BR (default) | EN | Context |
|------------|----------|-----------------|-----|---------|
| Auth — login | `auth.signIn` | Entrar | Sign in | Modal, comment gate |
| Auth — register | `auth.signUp` | Cadastrar-se | Sign up | Modal |
| Auth — logout | `auth.signOut` | Sair | Sign out | Menu |
| Menu — writing hub | `menu.writing` | Escrita | Writing | User menu → `/writing` |
| Menu — manage hub | `menu.manage` | Gerenciar | Manage | User menu → `/manage` |
| Menu — account hub | `menu.account` | Conta | Account | User menu → `/account` |
| Menu — review hub | `menu.review` | Revisão | Review | User menu (editor) → `/editor` |
| Menu — administration hub | `menu.administration` | Administração | Administration | User menu (admin) → `/administration` |
| Breadcrumb — home | `breadcrumb.home` | Início | Home | Public pages root segment |
| Auth — forgot password link | `auth.forgotPassword` | Esqueceu a senha? | Forgot password? | Login modal |

Further interface labels use the same four-column shape; canonical keys and EN/ES strings live in `src/main/resources/i18n/messages_en.json` and `messages_es.json`. Legacy rows below retain English reference text — prefer the JSON catalog when adding or changing copy.

| UI element | Label (EN reference) | Context |
|------------|----------------------|---------|
| Password recovery — title | Reset your password | `/password-recovery` |
| Password recovery — submit | Send reset link | Request form |
| Password recovery — success | If an account exists for that email, we sent reset instructions. | After request |
| Password reset — title | Choose a new password | `/password-recovery/reset` |
| Password reset — submit | Update password | Reset form |
| Password reset — success | Your password was updated. Sign in with your new password. | After reset |
| Password reset — invalid token | This reset link is invalid or has expired. Request a new one. | Invalid/expired token |
| Account — pending email | Verification pending for {email}. | Account security |
| Account — security saved toast | Account updated. | After account security save |
| Author appearance — saved toast | Appearance updated. | After appearance save |
| Blog settings — description hint | Markdown supported (bold, links, lists). | Blog settings form description field |
| Profile — email verification sent | Check your new email to confirm the address change. | After email change request |
| Profile — email verified | Email address updated. | After verification |
| Account email — password changed subject | Your contraponto password was changed | Security notice |
| Account email — reset subject | Reset your contraponto password | Password recovery |
| Account email — verify email subject | Confirm your new email address | Email verification |
| Account email — email changed subject | Your contraponto email address was changed | Notice to old address |
| Write — save | Save draft | Write toolbar |
| Write — publish | Publish | Write toolbar |
| Blog audience — follow (off) | Follow | Blog page, guest |
| Blog audience — follow (on) | Following | Blog page |
| Blog audience — email (off) | Subscribe by email | Blog page |
| Blog audience — email (on) | Subscribed | Blog page |
| Post — editor feature | Featured / ★ Featured | Post action bar, review row |
| Post — editor not featured | ☆ Not Featured | Review row |
| Post — code block copy | Copy | Code block toolbar |
| Post — code block copied | Copied | After successful copy |
| Home / blog hero | Featured | Category label on featured card |
| Pagination — public lists | Load more | Home, blog grid, search |
| Library tab | Drafts | Library |
| Library tab | Published | Library |
| Notifications empty | No notifications yet. Follow blogs to see new posts here. | Notifications page |
| Notifications overlay empty | No notification | Notification overlay |
| RSS feed link | RSS | Blog, tag, serie, home, footer |
| Dismiss notification (button) | Dismiss | Notification overlay row |
| Close notification overlay (button) | Close (×, aria-label) | Notification overlay header |
| View all notifications (link) | View all notifications | Notification overlay footer |
| Menu — editor | Featured Posts | Links to `/review` |
| Dashboard stat | Published posts | Dashboard card |
| Dashboard — blog selector | Blog | Analytics scope |
| Dashboard — month navigation | Previous month / Next month | Analytics toolbar |
| Dashboard — compare views | Compare with previous month / Hide comparison | Views chart toggle |
| Dashboard chart | Daily views | Views bar chart heading |
| Dashboard chart | Daily reading time | Reading time bar chart heading |
| Dashboard chart | New followers | Followers bar chart heading |
| Dashboard chart | New email subscribers | Subscribers bar chart heading |
| Dashboard summary | {n} views this month | Views chart total |
| Dashboard summary | {duration} reading time this month | Reading time chart total (humanized hours/minutes) |
| Post — average reading time | Avg reading time: {duration} | Post page metadata (when sessions exist) |
| Dashboard summary | +{n} new this month · {m} followers total | Followers chart |
| Dashboard summary | +{n} new this month · {m} subscribers total | Subscribers chart |
| Comment moderation | Approve / Reject | Post owner (implicit in moderation UI) |
| Custom page — published badge | Published | Manage list |
| Image control — page title | Images | `/blogs/{id}/images` |
| Image control — empty | No images uploaded for this blog yet. | Image list |
| Image control — alt field | Alt text | Image row form |
| Image control — updated toast | Image updated. | Alt save |
| Post — version (metadata) | Version {n} | Post page metadata trigger |
| Post — version badge | current | Metadata and modal list (latest snapshot) |
| Post — change history modal | Change history | Modal title |
| Post — change details | Changes from version {n} | Expandable diff summary in modal |
| Post — serie nav aria | Series navigation | On-post serie parts list |
| Post — serie part count | Series of {n} parts | Subtitle under serie title on post page |
| Author appearance — picture field | Profile picture | Author appearance |
| Author appearance — default banner field | Default blog banner | Author appearance |
| Author appearance — display name field | Display name | Author appearance |
| Writing hub — blogs nav | Blogs | Writing left nav |
| Manage hub — blogs nav | Blogs | Manage left nav (editors) |
| Blog manage — banner field | Blog banner | Blog edit form |
| Profile/blog — remove image | Remove | Image upload areas |
| Git sync — history page title | Git sync history | `/blogs/{id}/git-sync` |
| Git sync — view history link | View sync history | Blog manage Git section |
| Git sync — succeeded | Sync succeeded | Run list/detail badge |
| Git sync — failed | Sync failed | Run list/detail badge |
| Git sync — partial | Sync partially completed | Run list/detail badge |
| Git sync — skipped | Sync skipped | Run list/detail badge |
| Git sync — how to fix | How to fix | Detail entry column |
| Git sync — data loadable | Data loadable | Detail summary |
| Git sync — repository readable | Repository readable | Detail summary |
| Git sync — notification success | Git sync succeeded for {blog} | In-app notification |
| Git sync — notification failure | Git sync failed for {blog} | In-app notification |

Toast messages and validation errors should describe the domain action (e.g. "Cannot follow or subscribe to your own blog") in plain language consistent with the terms above.

---

## Domain events

| Event | When fired | Typical reaction |
|-------|------------|------------------|
| `PostPublishedEvent` | After a new or changed publication snapshot is committed | Notify followers; email subscribers |
| `PostGitSyncRequestedEvent` | After draft save or publish when blog has Git enabled | Export post to remote; record **Git sync run** |
| `CustomPageChangedEvent` | After custom page create/update/delete | Refresh `CustomPageCache` |
| `notificationsChanged` (HTMX) | After dismiss notification or mark all read | Refresh notification bell badge; reload open overlay |

---

## Business rules (invariants)

1. Every **user** has exactly one **main blog** (created at registration).
2. A **post** belongs to exactly one **blog**; **author** is derived from blog owner.
3. Post **slug** is unique per author (across all their blogs).
4. Blog **slug** is unique per owner.
5. Only **published** posts are visible to readers; **drafts** only to author (and platform editors where implemented).
6. **Publish** creates a monotonically increasing **publication snapshot** version per post.
7. Republishing identical content does not create a duplicate snapshot or re-fire notifications.
8. A user cannot **follow** or **subscribe by email** to their **own blog**.
9. **Blog audience** row is deleted when both follow and email subscribe are off.
10. **Follow** drives in-app **notifications** on publish; **subscribe by email** drives email (with deduplication log).
11. **Comments** are allowed only on **published** posts.
12. Non-owner **comments** start **Pending**; owner comments are auto-**Approved**.
13. Only the **post owner** may **moderate** (approve/reject) comments.
14. **Replies** require an **Approved** parent comment.
15. **Rejected** comments are hidden from everyone except moderation flows.
16. **Featured** posts appear on the **home page**; toggling is immediate (no confirmation).
17. Only the **blog owner** may change blog settings (name, slug, description, Git, **blog banner**, active flag on the edit form). **Editors** and **administrators** may **deactivate** another user's **secondary** blog but cannot edit blog fields.
18. **Effective blog banner** is resolved at display time; new secondary blogs copy the owner's **default blog banner** FK at creation when set (same `Image` row, not a duplicate file).
19. **Custom pages** served publicly must be **published** and present in cache after changes.
20. Public URLs for posts and custom pages must use `PostEndpoint.extractUrl` and `CustomPagePaths.publicUrl` — never ad-hoc path building.
21. **Password recovery** always responds with the same success message whether or not the email is registered (no email enumeration).
22. **Password reset tokens** are single-use, expire after a configured interval, and are invalidated when a new token of the same type is issued for the same user.
23. **Inactive users** cannot complete password recovery.
24. **Email change** keeps the confirmed email until the user verifies the **pending email**; another account cannot claim an email already used or pending elsewhere.
25. Changing a password (self-service reset, profile, or **user administrator**) sends a **password changed** **account email** to the user's current confirmed email; the email never contains the new password.
26. After a successful password reset, all **sessions** for that user are invalidated.

---

## Aggregates (summary)

| Aggregate | Root | Main children / links |
|-----------|------|------------------------|
| **User** | `User` | Blogs (owned), roles, blog audience rows |
| **Blog** | `Blog` | Posts, series, custom pages, Git settings |
| **Post** | `Post` | Tags, publication snapshots, live publication, comments |
| **Tag** | `Tag` | Global; referenced by posts and snapshots |
| **Custom page** | `CustomPage` | Optional blog scope |
| **Blog audience** | `BlogAudience` | User + blog pair |
| **Notification** | `Notification` | Recipient-centric inbox item |

---

## Mapping to code

| Domain area | Package |
|-------------|---------|
| Users & roles | `dev.vepo.contraponto.user` |
| Blogs | `dev.vepo.contraponto.blog` |
| Posts & publications | `dev.vepo.contraponto.post` |
| Tags | `dev.vepo.contraponto.tag` |
| Series | `dev.vepo.contraponto.serie` |
| Custom pages | `dev.vepo.contraponto.custompage` |
| Notifications & audience | `dev.vepo.contraponto.notification` |
| Comments | `dev.vepo.contraponto.comment` |
| Git sync | `dev.vepo.contraponto.git` |
| Auth (tokens, account email, recovery) | `dev.vepo.contraponto.auth` |
| Auth & profile forms | `dev.vepo.contraponto.components.forms` |
| Profile page | `dev.vepo.contraponto.components` |
| Editor review | `dev.vepo.contraponto.admin` |

Access helpers (not aggregates): `BlogAccess`, `UserAccess`, `CustomPageAccess`.

---

## Checklist for changes

Before implementing a feature or fix:

1. Read **Ubiquitous Language** and **Business rules**.
2. Decide if the change needs new terms, UI labels, events, or rules — update this file first if yes.
3. Name classes, methods, tests, and templates with domain terms from this document.
4. After implementation, re-read this spec and sync any drift.
