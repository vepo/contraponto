# Contraponto UI/UX Guidelines

*Version 1.0 – A newspaper‑style blogging platform*

---

## 1. Introduction

Contraponto is a publishing platform designed to combine the elegance of a classic newspaper with the speed of a modern web application. Every design decision aims to **maximize readability, guide the eye naturally, and give writers full control over their content** without interfering with the reading experience.

---

## 2. Design Principles

### 2.1 Typography-driven hierarchy
- **Serif fonts** (Cormorant Garamond, Playfair Display) for headings, titles, and body copy to evoke a timeless editorial feel.
- **Sans‑serif font** (Inter) for UI elements, metadata, and navigation – ensuring clean contrast.
- Font sizes follow a modular scale: `16px` base for body, `1.1rem` for article content, `2.25rem` for main post titles on desktop. Full token table: [ui-palette.md](ui-palette.md).

### 2.2 Color restraint (muted editorial)
- Primary brand colour: `#3a6b3e` (sage green) – links, buttons, accents.
- Accent colour: `#7a3344` (muted burgundy) – drop caps, pull‑quote borders, critical alerts.
- Neutrals: warm paper `#f6f5f2` (background), `#1a1a18` (text), `#3a3a37` / `#5a5a56` (secondary/muted).
- **Canonical reference:** [ui-palette.md](ui-palette.md) — check before adding UI colours.

### 2.3 Compact, readable layout
- Content width `1080px` (home), `760px` (narrow forms/articles).
- Grid‑based post cards; spacing tokens from `0.375rem` to `3rem` (see ui-palette.md).

### 2.4 Subtle interactions
- Hover states use colour shifts and soft shadows, never abrupt changes.
- Transitions are smooth (`0.2s`) and never distract from content.

### 2.5 Mobile first
- Base CSS targets the smallest viewport; tablet and desktop add columns, sticky sidebars, and margin rails via `min-width` breakpoints (see [ui-palette.md](ui-palette.md): `--bp-sm` 480px, `--bp-md` 768px, `--bp-lg` 1024px, `--bp-xl` 1280px).
- All main functions are accessible on mobile with stacked layouts, hidden secondary chrome (e.g. header button labels, compact locale picker), and touch targets of at least `--tap-target-min` (44px).

---

## 3. Global Components

### 3.1 Header
- Sticky top bar with two columns: **left** (optional sidebar menu + logo) and **right** (search, write, notifications, locale, user menu or auth buttons).
- **Logo:** “contraponto” in Playfair Display, links to home — left-anchored beside the optional hamburger.
- **Left:** optional sidebar menu button and site logo.
- **Right:** search, write button (if logged in), notifications, locale picker, user menu or Sign Up / Sign In buttons. On `/write`, Publish and Save draft live in the editor **write-actions** bar, not the header.

### 3.2 Footer
- Darker background (`#fafafa`), multi‑column layout with links to explore, connect, legal, and dynamically loaded **Custom Pages** (global, placement `FOOTER` or `BOTH`).
- Copyright line at the bottom.

### 3.3 Search Modal
- Triggered by the search icon in the header.
- Contains a search input that autofocuses; results appear inline with subtle animation.
- Initial state shows a friendly “Find the perfect story” placeholder.
- Live filtering (`keyup` with 300ms delay) – no page reload.
- “Advanced search →” link opens the dedicated search page with pagination.

### 3.4 Authentication Modals
- Separate modals for Sign‑Up and Sign‑In, toggled via links in the modal footer.
- Form validation is real‑time: error messages appear only after a field loses focus (if invalid).
- Submit button is disabled until all required fields are valid.

### 3.5 User Menu
- Dropdown showing avatar, name, email.
- Items: **My Blog**, **Writing**, **Manage**, **Account**, **Review** (editor only), **Administration** (user administrator only), **Sign out**.
- Each hub link opens a **left-sidebar shell**: breadcrumb, sticky section nav, and the active section panel (no hub-level duplicate title).
- Default section loads on first open (e.g. Manage → Dashboard, Writing → Library, Review → Featured Posts).
- Section URLs are bookmarkable (`/writing/library`, `/writing/blogs`, `/writing/appearance`, `/manage/dashboard`, `/manage/pages`, `/account/security`, `/editor/tags`, …).
- **Writing** hub sidebar: **Library**, **Images**, **Blogs**, **Appearance**; compose via header **Write** (`/write`).
- Panel list headers use **hub-panel__header** with compact primary actions (`btn--small`).
- Desktop: section nav stays visible while the panel scrolls (`position: sticky`). Mobile: horizontal section tabs under the site header.
- Header shortcuts unchanged: **Write** button → `/write`; notifications bell → overlay dropdown (full inbox via **View all notifications**).
- The dropdown closes on escape or when clicking outside.

### 3.6 Breadcrumbs
- Shown at the top of every full-page surface (inside `main`, above the page title).
- **Post pages** show the breadcrumb on its own row (same spacing as manage forms), then a **page-top** row for Edit / Feature / Follow / Share aligned right (stacks on narrow screens).
- **Blog home** header includes the same **share actions** pill (LinkedIn, Bluesky, Copy) beside follow/subscribe.
- Public reading: root segment **Home** (`/`), then author/blog/post segments as applicable.
- Logged-in hubs: **hub name** links to the hub default section; current **section name** is the last segment (e.g. Review › Tags).
- Deep manage/editor forms (blog edit, tag edit) keep full-page breadcrumbs from the hub section to the form.
- The current page is the last segment (not linked); ancestors use HTMX navigation.
- HTMX partials (grids, tabs, modals) do not include breadcrumbs.

### 3.7 Toast Notifications
- Non‑blocking, positioned bottom‑right.
- Success (green) and error (red) variants.
- Appear on publish, draft save, or critical errors.

---

## 4. Home Page

**Purpose:** Showcases curated featured posts to all visitors.

- **Featured hero:** The first featured post gets a large two‑column layout (cover image + title/excerpt) at the top.
- **Guest introduction masthead:** Unauthenticated visitors see a short editorial welcome above the hero (message explains curated featured posts). Dismissible per browser; hidden for signed-in users.
- **Post grid:** Below, the remaining featured posts are displayed in a three‑column card grid.
- **Load more:** At the bottom, a “Load more” button fetches the next page of featured posts via HTMX without a full page refresh.
- **Visual priority:** The featured hero uses a larger title and a drop‑shadow on the cover. The grid uses smaller cards with a uniform image height.
- **Empty state:** If no posts are featured, the page shows a friendly message inviting editors to feature content.

---

## 5. Blog Page (User’s Blog)

**URL:** `/{username}` (default blog) and `/{username}/{blogSlug}` (additional blogs).

- **Blog header:** The blog name (`Blog.name`) is displayed as a large title. When the blog name differs from the author's display name, a **por {display name}** byline links to the author profile. Optional biography or blog description below.
- **Featured post:** If the blog has a featured post, it uses the same hero layout as the home page.
- **Post grid:** All published posts from that blog in a card grid.
- **Empty state:** “No posts published yet” with a link for the blog owner to start writing.
- The page respects the same newspaper styling.

---

## 6. Post Page

**URL:** `/{username}/post/{slug}` (or `/{username}/{blogSlug}/post/{slug}`).

- **Cover image:** Full‑width, rounded corners, max height 500px.
- **Title:** Playfair Display, `3rem`, left‑aligned.
- **Byline:** Author name (link to their blog), followed by publication/update dates and view count.
- **Content:** Clean typography with drop caps on the first paragraph, proper heading hierarchy (H2 with left red border, H3 italic), pull‑quotes, and horizontal rules with a diamond symbol.
- **Action bar** (visible to author/editor):
  - Edit button (pencil icon) – opens the post in the editor.
  - Feature toggle (star icon) – only for editors; clicking toggles the featured status inline without page reload.
- Content is rendered from Markdown/AsciiDoc; code blocks use syntax highlighting.

---

## 7. Search Experience

### 7.1 Quick Search (Modal)
- Opens from header icon.
- Live results appear as the user types; each result shows title, author, date, and excerpt.
- Clicking a result navigates to the post.

### 7.2 Advanced Search Page
- Dedicated `/search` page with a larger input and a “Search” button.
- Results are paginated (Previous / Next).
- Empty state: “No results found for …”.
- URL updates with query for shareability.

---

## 8. Authentication

### 8.1 Sign‑Up Modal
- Fields: Username (3‑20 chars), Full Name, Email, Password.
- Real‑time validation: required, email format, length.
- Success redirects to the previous page (or home).

### 8.2 Sign‑In Modal
- Fields: Email or Username, Password.
- Error messages display inline (e.g., invalid credentials).
- **Forgot password?** links to `/password-recovery`.

Both modals are accessible via the header buttons.

### 8.3 Password recovery
- **Request page** (`/password-recovery`): email field, **Send reset link**, generic success message.
- **Reset page** (`/password-recovery/reset?token=…`): new password + confirm, **Update password**; invalid/expired token shows a clear error and link to request again.

---

## 9. Profile Settings

**URL:** `/account/security`
- A narrow‑width form with fields: Full Name, Email, Current Password (required for changes), New Password (optional), Confirm New Password.
- Email change shows **Verification pending for {email}.** until the new address is confirmed.
- Validation rules: password mismatch, email format, etc.
- “Save Changes” button.
- Success/error messages appear above the form.

---

## 10. Write / Editor

**URL:** `/write` (new post) or `/write/draft/{id}` (edit existing).

- **Header:** “Publish” and “Save Draft” buttons, plus a “Write” link to exit.
- **Title field:** Large, borderless, placeholder “Story title…”.
- **Cover image upload:** Drop‑zone with preview; drag & drop or click to upload.
- **Slug / description:** Simple text fields.
- **Format selector:** Markdown / AsciiDoc toggle with a vintage dropdown.
- **Toolbar:** Bold, italic, underline, headings, lists, quote, code, link, image insertion, and preview toggle.
- **Editor area:** Monospace font, line height 1.7, minimal border.
- **Live preview:** Toggled via toolbar button; renders formatted text instantly.
- **Saving:** “Save Draft” shows toast; “Publish” triggers validation and may redirect to the published post.
- Only the author can edit; button visibility is controlled by `data-disable-pattern`.

---

## 11. Dashboard

**URL:** `/manage/dashboard`
- **Analytics (per blog):** Blog selector; month navigation (Previous / Next); optional “Compare with previous month” on daily views. Three bar charts: Daily views, New followers, New email subscribers. Each chart shows month totals; audience charts show current totals.
- **Stats cards:** Number of drafts and published posts (clickable links to library filtered by tab).
- **Recent drafts / published:** Lists with title, last‑updated date, views (for published).
- **Quick action:** “✍️ Write a new story” button.

Analytics load via HTMX (`GET /manage/dashboard/components/analytics`) on page load and when blog, month, or compare changes.

---

## 12. Library

**URL:** `/writing/library`
- Tabs: **Drafts** / **Published** – each loads content dynamically via HTMX.
- Each card shows title (or “Untitled”), excerpt, metadata, and action buttons (Edit, Delete for drafts).
- Empty state: “No drafts yet” with a link to start writing.

---

## 13. Review Page (Editor)

**URL:** `/review` (only accessible to users with `EDITOR` role).
- Displays a list of all published posts.
- Each row: title, author, date, and a star toggle button.
- Clicking the star instantly marks/unmarks the post as **Featured** and updates the row’s appearance (filled/unfilled star).
- Provides a central place to curate the home page content.

---

## 14. Custom Pages

**URL:** `/page/{slug}` (global) or `/page/{username}/{slug}` (per blog).
- A simple page layout with a title and rendered HTML content.
- Placements: `FOOTER`, `SIDEBAR`, `BOTH`, or `NONE`.
- Footer links are loaded dynamically via HTMX.
- Editors can manage pages via the admin interface (not yet detailed).

---

## 15. Accessibility & Responsiveness

- All interactive elements have visible focus outlines.
- Forms use proper label‑input associations.
- Modals trap focus and close with Escape.
- Images have `alt` attributes; SVGs are used for icons.
- **Breakpoints** (canonical values in [ui-palette.md](ui-palette.md)):

| Token | Width | Typical enhancement |
|-------|-------|---------------------|
| (base) | &lt; 480px | Single-column grids, compact header, locale picker hidden in header |
| `--bp-sm` | 480px+ | Header button labels; write header labels |
| `--bp-md` | 768px+ | Two-column featured hero; hub vertical nav; footer link columns |
| `--bp-lg` | 1024px+ | Three-column post grids; full article title scale |
| `--bp-xl` | 1280px+ | Browse margin rails; related-posts aside |

- **Mobile defaults:** single-column post grids; smaller article titles; horizontally scrollable editor toolbar; header icon labels hidden until `--bp-md`.
- **Touch targets:** icons, tabs, and primary buttons meet `--tap-target-min` (44px).
- **Safe areas:** sticky header and toasts respect `env(safe-area-inset-*)` on notched devices.
- Sufficient colour contrast (WCAG AA compliant).

---

## 16. CSS and class names

Implementation details (BEM blocks, `main.css` / `manage.css` / `write.css` bundles, button modifiers) live in **[ui-elements.md](ui-elements.md)**. This document describes UX only.

---

## 17. List pagination

| Context | Who | Control | Page size | Shared partial |
|---------|-----|---------|-----------|----------------|
| Home, blog, tags | Visitors | **Load more** (HTMX append) | 12 | `components/load-more-posts.html` |
| Search | Visitors | **Load more** | 20 | Search-specific HTMX swap |
| Library, CRUD lists, review, notifications, subscriptions | Logged-in users | **Previous / Next** with totals | 20 | `components/manage-pagination.html` |
| Dashboard | Author | Recent previews only | 5 | — (links to full paginated lists) |

**Reading lists** use `#more-posts`, `hx-swap="outerHTML"`, and 1-based `?page=` on fragment endpoints.

**Managing lists** show `N items · Showing X–Y · Page P of T` and navigate with full-page `?page=` (or HTMX fragment target for library tabs).

---

## 18. Notifications

- **Bell** in the header shows an unread count badge and opens a **notification overlay** (dropdown) with the latest unread items — not a direct navigation to the inbox.
- **Overlay**: unread count in header, per-row **Dismiss**, panel **Close** (×), **Mark all read**, and **View all notifications** link to the full inbox.
- Badge and open overlay refresh on `notificationsChanged` (after dismiss / mark-all-read) and on a configurable poll interval.
- **Inbox** at `/account/notifications`: full list with read/unread styling; “Mark all read” when unread exist.
- **Empty state** (inbox): invite users to follow blogs; overlay empty state: **No notification** (placeholder shown immediately while loading).
- Pagination: manage style (Previous / Next, 20 per page).

---

## 19. Blog audience (follow & email)

On public blog pages, controls below the blog header:

- **Follow** / **Following** — toggles in-app notifications on new posts.
- **Subscribe by email** / **Subscribed** — email on publish (deduplicated).
- Guests see Sign in when action requires auth; owners do not see controls on their own blog.

Compact outline buttons; active state uses primary border colour.

---

## 20. Tags and series

- **Tag page** (`/tags/{slug}`): `#` prefix on title, optional description, editor “Edit tag” when permitted; post grid matches blog styling.
- **Serie page** (`/serie/{slug}`): “Serie” label, title, post grid.
- **Post in a serie**: after metadata, a **Series navigation** block lists all published parts (oldest first); the current part is highlighted and not linked; other parts link via HTMX.
- **Manage tags** (`/tags/manage`): same `pages-manage` list shell as other CRUD indexes.

---

## 21. Comments

- **On post**: threaded list, reply forms, Sign in gate for guests; owner moderation actions (approve/reject) for pending comments.
- **Manage** (`/comments`): post owner reviews pending/rejected comments with approve/reject actions.

---

## 22. Subscriptions

**URL:** `/subscriptions` — lists blogs the user follows or receives by email, with links to each blog. Empty state when none. Manage pagination.

---

## 23. Post change history

On the post page (all readers): **Version {n}** with a history icon in `article-page__metadata` (live snapshot, **current** badge). Click opens a **Change history** modal listing publication versions with timestamps; each non-initial version can expand to show diffs from the previous version.

---

## 24. Interface language (i18n)

- **Default:** Brazilian Portuguese (`pt-BR`) in templates; each interface string has a `data-i18n` key.
- **Secondary:** English and Spanish via header/footer **flag dropdown** (`POST /forms/locale`, cookie `contraponto_locale`). Click the flag, then choose a language.
- **Markup:** put `data-i18n` on leaf text (e.g. `<span>` inside buttons/menus). Use `data-i18n-attr` for placeholders and `aria-label` on inputs — never set `textContent` on form controls.
- **Account hub:** same switcher plus short hint that only chrome is translated (author content stays as written).
- **Do not translate:** post bodies, comments, blog descriptions, custom page content from the database, transactional emails.

---

## 25. Summary

Contraponto’s UI marries classic editorial design with modern interactivity. Every component – from the sticky header to the inline featured toggle – is built to feel responsive, intentional, and calm. The platform treats content as the star, while providing writers and editors with just enough functional power to shape the reading experience without ever overwhelming them.