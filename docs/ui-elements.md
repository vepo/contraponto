# UI Elements catalog

Canonical reference for **CSS class names** and **stylesheet bundles**. For **design tokens** (colors, spacing, typography), see [ui-palette.md](ui-palette.md). For UX flows and page narratives, see [ui-guidelines.md](ui-guidelines.md). For user-visible copy, see [domain-specification.md](domain-specification.md) § UI labels.

---

## Naming conventions

| Pattern | Example | Use |
|---------|---------|-----|
| Block | `article-card` | Standalone component |
| Element | `article-card__title` | Part of a block |
| Modifier | `article-card--featured` | Variant on block or element |
| Utility | `u-hidden` | Single-purpose helpers (`u-` prefix) |

- Use **kebab-case** only.
- Do not introduce new classes without adding a row to the registry below (or a clearly related block).
- Prefer extending an existing block over one-off names.

---

## Stylesheet bundles

Loaded from [head.html](../src/main/resources/templates/components/head.html) in this order:

| File | Scope |
|------|--------|
| `style/fonts.css` | Self-hosted `@font-face` (no layout) |
| `style/main.css` | Design tokens, reset, global shell (header, footer, auth modals), **public reading** (home, blog, post, search, tags, series, on-post comments) |
| `style/manage.css` | **Logged-in operations**: profile, dashboard, library, review, CRUD lists (`pages-manage`), notifications inbox, subscriptions, manage pagination |
| `style/write.css` | Write editor only (`/write`, `/write/draft/*`) |
| `style/third-party/*` | Vendored (e.g. highlight.js) |

When adding styles, put them in the bundle that matches the primary surface. Shared primitives (`.btn`, `.form-group`, `.container`) live in **main.css**.

---

## Shared primitives (main.css)

### Layout

| Class | Purpose |
|-------|---------|
| `.container` | Max width 1080px (`--container-max`) |
| `.container-narrow` | Max width 760px (`--container-narrow`) |
| `.sr-only` / `.u-sr-only` | Screen-reader only text |

### Buttons (always `btn` + modifier)

| Modifier | Purpose |
|----------|---------|
| `.btn--primary` | Primary action (green fill) |
| `.btn--secondary` | Neutral bordered |
| `.btn--outline` | Transparent + border (comments, audience, post actions) |
| `.btn--small` | Compact padding (library rows, manage actions) |
| `.btn--large` | Dashboard CTA |
| `.btn--danger` | Destructive |
| `.btn--ghost` | Minimal chrome |
| `.btn--block` | Full width (auth submit) |
| `.btn--header` | Header Write CTA |
| `.btn--auth-login` / `.btn--auth-signup` | Auth modal triggers |
| `.btn--icon` | Icon-only control |
| `.btn--edit` | Post edit link style |
| `.btn--featured` | Review row featured state |
| `.btn--star-active` | Featured toggle active on post |

### Forms

| Block | Elements / modifiers |
|-------|-------------------|
| `.form-group` | Standard field wrapper; `.form-group--error`, `.form-group--checkbox` |
| `.error-message` | Validation text (with parent `--error` when applicable) |
| `.pages-form` | Manage CRUD forms (extends form-group inputs) |

### Utilities

| Class | Purpose |
|-------|---------|
| `.u-hidden` | `display: none` (toasts, previews, HTMX helpers) |
| `.u-invisible` | Hidden but occupies space |
| `.u-mt-*`, `.u-mb-*`, `.u-stack`, `.u-cluster` | Spacing helpers |

### Feedback

| Block | Purpose |
|-------|---------|
| `.toast` | Global toast container (position/animation only; styles in **main.css**) |
| `.toast--success` | Success message pill (`var(--color-primary)`) |
| `.toast--error` | Error message pill (`var(--color-accent)`) |
| `.page-progress` | HTMX global indicator (`#global-indicator`) |
| `.loading-spinner` | Inline loading text (library tab) |
| `.spinner` | Animated spinner (optional) |

### Locale picker (main.css)

| Block | Purpose |
|-------|---------|
| `.locale-picker` | Language dropdown root (header, footer, Account hub) |
| `.locale-picker--compact` | Flag-only trigger + dropdown panel |
| `.locale-picker--list` | Always-visible option list (Account hub) |
| `.locale-picker__trigger` | Opens dropdown (compact mode) |
| `.locale-picker__dropdown` | Dropdown panel |
| `.locale-picker__option` | Locale row (flag + name) |
| `.locale-picker__option--active` | Selected locale |
| `.locale-picker__flag` | Flag image (24×18) |

### Pagination

| Block | Context | Bundle |
|-------|---------|--------|
| `.load-more` | Public “Load more” | main |
| `.manage-pagination` | Logged-in numbered pages | manage |

### Breadcrumb (main.css)

| Block | Purpose |
|-------|---------|
| `.breadcrumb` | Page trail container |
| `.breadcrumb__list` | Ordered list |
| `.breadcrumb__item` | One segment |
| `.breadcrumb__link` | Linked ancestor (HTMX nav) |
| `.breadcrumb__text` | Non-linked middle segment |
| `.breadcrumb__current` | Current page (`aria-current="page"`) |

---

## main.css — reading & shell

| Block | Templates | Notes |
|-------|-----------|-------|
| `site-header`, `icon-btn`, `logo`, `nav-*` | `components/header.html` | Sticky header |
| `site-footer`, `footer__*` | `components/footer.html` | |
| `sidebar`, `sidebar-overlay` | `components/header.html`, `MenuEndpoint/menu.html` | Drawer nav |
| `modal`, `modal__*` | `AuthModal/modal.html`, `SearchEndpoint/modal.html` | |
| `user-menu`, `user-menu__*` | `MenuEndpoint/menu.html` | |
| `article-card`, `article-meta` | Home/blog/tag grids | Post cards |
| `featured`, `featured__*` | Home/blog featured hero | |
| `featured__grid--no-cover` | `HomeEndpoint/featured.html` | Single-column hero when post has no cover |
| `posts-grid`, `load-more` | Grid partials | |
| `article-page`, `article-page__*` | `PostEndpoint/post.html` | Post reading |
| `.imageblock` (in `.article-page__content`, `.write-preview`) | Asciidoctor block images | Centered figure, caption in `.title`; styles in main.css / write.css |
| `.verseblock` (in `.article-page__content`, `.write-preview`) | Asciidoctor verse / poetry | Lines in `pre.content`, attribution in `.attribution`; styles in main.css / write.css |
| `.tableblock` (in `.article-page__content`, `.write-preview`) | Asciidoctor tables | `table.tableblock` with caption `.title`, cell text in `p.tableblock`; horizontal scroll via `.sectionbody:has(> table.tableblock)`; styles in main.css / write.css |
| `image-lightbox`, `image-lightbox__*` | `#image-lightbox` in `components/head.html` | Post body image expand overlay; `main.js` + main.css |
| `code-block`, `code-block__copy` | Injected by `CodeCopyManager` on `.listingblock` / `pre` | Copy post and write-preview code; `main.js` + main.css |
| `post-serie-nav`, `post-serie-nav__*` | `PostEndpoint/serie-nav.html` | Serie parts list on post page |
| `article-page__version`, `article-page__version-icon` | `PostEndpoint/post.html` | Version history trigger in metadata |
| `post-history`, `post-history__*` | `PostEndpoint/history-list.html`, `historyModal.html` | Version history modal list |
| `modal__container--history` | `PostEndpoint/historyModal.html` | Wider change-history dialog |
| `post-tags`, `post-tags__*` | `components/post-tags*.html` | |
| `comment`, `comment-list`, `comment-form`, `comment-replies` | Comment components | On-post only |
| `post-highlights`, `post-highlights__actions` | `HighlightComponentEndpoint/highlights.html` | Lazy-loaded below article; respond / my highlights actions |
| `highlights-selection-bar` | `HighlightComponentEndpoint/highlights.html` | Fixed popover on text selection (**Highlight**, **Add note**, **Remove highlight**); moved to `document.body` while visible |
| `post-highlight`, `post-highlight--personal`, `post-highlight--official`, `post-highlight__badge` | Applied by `highlight.js` | Inline marks in `.article-page__content` |
| `highlight-note-modal`, `highlight-note-modal__*` | `HighlightNoteFormEndpoint/highlightNoteModal.html` | Note dialog loaded into `#modal-container` |
| `post-response-banner`, `post-responses-section`, `post-responses-section__*` | `HighlightComponentEndpoint/highlights.html` | Post response link-back and approved responses list |
| `blog-audience` | `BlogAudienceComponentEndpoint/audienceControls.html` | Follow / subscribe; no top margin inside `article-page__actions` (blog header keeps spacing below title) |
| `notification-bell`, `notification-bell__badge` | `NotificationBadgeEndpoint/badge.html` | Header bell + unread count |
| `notification-menu`, `notification-menu__dropdown` | `NotificationBadgeEndpoint/badge.html` | Overlay shell |
| `notification-overlay__*` | `NotificationOverlayService/overlay.html` | Overlay list, dismiss, footer |
| `search-page`, `search-form`, `search-result`, `search-modal__*` | Search endpoints | |
| `user-blog-page`, `user-blog__*` | `BlogEndpoint/home.html` | Author blog |
| `content-render`, `content-render--youtube`, `content-render--gist`, `content-render--github`, `content-render--twitter`, `content-render--error` | Post body render plugins | YouTube, Gist, GitHub, Twitter embeds |
| `tag-page-*`, `serie-page-*` | Tag/serie pages | |
| `custom-page`, `custom-page__*` | `CustomPageEndpoint/page.html` | Static pages |
| `error-page`, `error-*` | `error.html` | |

---

## manage.css — logged-in operations

| Block | Templates | Notes |
|-------|-----------|-------|
| `profile-form` | `AccountSecurityEndpoint/panel.html`, `AuthorAppearanceEndpoint/panel.html` | Account security and author appearance |
| `image-upload-area` | `components/image-upload-area.html` | Reusable banner/profile image picker |
| `user-blog-header__banner` | `BlogEndpoint/home.html` | Blog home hero banner |
| `stat-card`, `recent-*` | `DashboardEndpoint/panel.html` | Manage hub dashboard panel |
| `dashboard-analytics`, `dashboard-chart` | `DashboardEndpoint/analytics.html` | Per-blog charts (views, reading time, followers, subscribers); HTMX month nav |
| `library-tab`, `library-tabs` | `LibraryEndpoint/panel.html`, `LibraryEndpoint/tab.html` | Writing hub library |
| `draft-card`, `drafts-page` | `LibraryEndpoint/tab.html` | |
| `post-card` | Library published tab | |
| `review-page`, `review-row` | `ReviewEndpoint/*` | |
| `pages-manage`, `pages-manage__*` | User/blog/tag/custom page manage | Shared CRUD shell |
| `pages-form`, `user-roles` | Manage forms | |
| `notifications-page`, `notification-list` | `NotificationEndpoint/notifications.html` | |
| `subscriptions-page`, `subscription-list` | `SubscriptionEndpoint/subscriptions.html` | |
| `manage-pagination` | `components/manage-pagination.html` | |
| `hub-shell-page` | `NavigationHubService/shellPage.html`, `components/hub-shell-page.html` | Hub page chrome |
| `hub-layout`, `hub-layout--single`, `hub-nav__sticky`, `hub-nav`, `hub-nav__link`, `hub-nav__link--active`, `hub-panel` | `components/hub-shell.html`, `manage.css` | Sticky left nav + section panel |
| `hub-panel__header`, `hub-panel__title`, `hub-panel__subtitle` | `components/hub-panel-header.html`, hub panel templates, `manage.css` | Panel section title + compact CTA |
| `page-top`, `page-top__actions` | `components/page-top.html`, `PostEndpoint/post.html`, `main.css` | Post page actions row (breadcrumb above, same as manage) |
| `rss-feed-link`, `rss-feed-link__icon` | `components/rss-feed-link.html`, `main.css` | RSS syndication link (opens feed in new tab) |
| `page-meta` | `HomeEndpoint/home.html`, `main.css` | Right-aligned meta row (e.g. site RSS on home) |
| `user-blog-header__actions` | `BlogEndpoint/home.html`, `main.css` | Blog header row: audience controls + RSS |
| `image-control-hub__blog-select`, `image-control-hub__blog-name` | `ImageControlEndpoint/panel.html`, `manage.css` | Writing hub Images blog switcher |
| `nav-hub-page`, `nav-hub__*` | (deprecated) | Replaced by hub shell; remove when card templates are deleted |

---

## write.css — editor

| Block | Templates |
|-------|-----------|
| `write-page`, `write-header`, `write-main`, `write-form`, `write-form__*` | `WriteEndpoint/write.html` |
| `write-toolbar`, `write-editor`, `write-preview` | |
| `write-tags-picker` | Tag picker on write form |

---

## When adding UI

1. Read [domain-specification.md](domain-specification.md) — labels and domain terms.
2. Reuse a block from this catalog or add a row here first.
3. Add CSS to the correct bundle (`main`, `manage`, or `write`).
4. Update [ui-guidelines.md](ui-guidelines.md) if the change introduces a new page type or flow.
5. For lists, follow [contraponto-pagination.mdc](../.cursor/rules/contraponto-pagination.mdc).

---

## Audit

Run `scripts/audit-ui-classes.sh` to report template classes missing from CSS and unused selectors (informational).
