# Application Flow & Structure Guidelines

*This document describes the structural blueprint of the Contraponto publishing platform. It defines routes, their responsibilities, data sources, user roles, and internal navigation logic. It is intended for developers and system architects.*

> **Note (2026-05):** Several sections predate multi-blog publications, notifications, tags, series, Git sync, and RSS. For an up-to-date URL map and patterns, see [ARCHITECTURE.md](../ARCHITECTURE.md). Track doc updates in [conventions-checklist.md](conventions-checklist.md).

---

## 1. Overview

The application supports two primary modes: **guest reading** and **authenticated interaction**.  
- A **Guest** can browse the home page, individual blogs, posts, and search.  
- An **Authenticated User** can write, edit, and manage their own content, and optionally hold an **Editor** role for site‑wide curation.

The platform now supports **multiple blogs per user**. Every user has exactly one **default blog** (created automatically) and may own additional **secondary blogs**.

---

## 2. Route Map

### 2.1 Main Page (`GET /`)
- **Purpose:** Curated landing page for the entire platform.  
- **Data:** Only **published and featured** posts (across all blogs).  
- **Layout:**  
  - The newest featured post gets a **hero** section (large cover + excerpt).  
  - Remaining featured posts appear in a three‑column grid below.  
- **Pagination:** An HTMX “Load more” button loads further pages via `GET /components/home/grid?page=X`.  
- **Discovery links:** **Ver autores** (`GET /authors`) and **Ver blogs** (`GET /explore/blogs`) in the home `page-meta` row (beside the site RSS link).  
- **Empty state:** If no featured posts exist, a friendly message is shown (no special restrictions – visibility of featured flag is controlled by editors).

### 2.2 Blog Pages

#### 2.2.1 Default Blog (`GET /{username}`)
- Redirects internally to the user’s default blog if exactly one blog exists; otherwise shows the user’s public profile with a list of their blogs.  
- **Data:** All published posts from that blog, ordered by newest.  
- **Hero:** The most recent published post is displayed as featured for that blog.  
- **Grid:** Remaining posts in card format.  
- **Empty state:** “No posts published yet” – if the visitor is the blog owner, a call‑to‑action to write appears.

#### 2.2.2 Specific Blog (`GET /{username}/{blogSlug}`)
- Displays the specified blog’s published posts.  
- Same structure as default blog.  
- If the blog is not active or does not exist → 404 error page.

#### 2.2.3 Blog Sub‑components
- `GET /{username}/components/home/grid?page=X` – returns only the grid of posts (used for HTMX infinite scroll).  
- The blog page itself is a full HTML document.

### 2.3 User Public Profile (`GET /{username}` – when multiple blogs exist)
- If a user has more than one blog, visiting their username shows:  
  - **Header:** Display name, optional bio.  
  - **List of blogs:** each blog name links to the specific blog page.

### 2.4 Post Pages

#### 2.4.1 Primary Blog Post (`GET /{username}/post/{slug}`)
- Shows a single post from the user’s **default blog**.  
- If the post does not belong to the default blog → 404 (or redirect to the correct blog path).  
- **Visibility:** Published posts are always visible; drafts only visible to the author (with edit link).

#### 2.4.2 Secondary Blog Post (`GET /{username}/{blogSlug}/post/{slug}`)
- Shows a single post from a specific blog.  
- **Validation:**  
  - Blog must be active.  
  - Post must be published (or draft for owner).  

#### 2.4.3 Post Page Content
- The post page includes:  
  - Cover image (if present).  
  - Title, byline (author → link to blog), publication date, view count, average reading time (when sessions exist).  
  - Rendered content (Markdown/AsciiDoc).  
  - **Image lightbox:** clicking an inline image in the post body opens a larger view in an overlay (same URL); dismiss with ESC, the close control, or a click on the dark backdrop.  
  - **Code block copy:** fenced and listing code blocks show a **Copy** control that copies plain source to the clipboard (label becomes **Copied** briefly).  
- **Action bar** (all readers):  
  - **Share:** LinkedIn and Bluesky open the platform share/compose UI in a new tab with the canonical URL or share text; **Copy** puts title + URL on the clipboard (label becomes **Copied** briefly).  
  - **Follow / Subscribe** (when the blog has audience controls).  
  - **Save for later / read state** (signed-in readers).  
- **Action bar** (authenticated only):  
  - **Author:** Edit button (pencil) → `/write/draft/{id}`  
  - **Editor:** Star toggle to mark/clear the **featured** flag. Clicking toggles via PUT, and the button updates without full page reload.

#### 2.4.4 Post Interactions
- Featured flag toggled via `PUT /admin/posts/{id}/featured` (or a dedicated review endpoint).  
- View counts are tracked server‑side on each page load.
- Reading time is tracked client‑side: while a published post tab is **visible**, the browser sends a heartbeat every 5 seconds to `POST /forms/posts/{postId}/reading-time` (same `__view_session` cookie as views). Background or hidden tabs do not accumulate time.
- Post metadata shows **average reading time** across reading sessions when data exists.

---

## 3. Search

### 3.1 Quick Search (Modal)
- **Route:** `GET /search/modal` (returns modal HTML).  
- **Behavior:** Input fires `GET /search/results?q=...` on `keyup` (300ms debounce). Results are loaded into the modal’s `#modalSearchResults` div.  
- If the input is empty, an initial placeholder is shown.  
- **Enter** in the search field opens the advanced search page (`/search`, with `?q=` when the field has text); same navigation as the **Busca avançada** footer link (`main.closeModal` + `#btnGoToAdvanced`).  
- Clicking a result navigates away.

### 3.2 Search Page
- **Route:** `GET /search`  
- Contains a search form and an empty results container.  
- On page load (or user search), it fetches `GET /search/results?q=...&page=1` via HTMX into `#searchResults`.  
- Pagination uses HTMX to replace the results container with the next/prev page.

---

## 4. Authentication

- **Sign‑Up Modal:** `GET /auth/modal?mode=signup` → renders inside `#modal-container`.  
- **Sign‑In Modal:** `GET /auth/modal?mode=login`  
- Forms POST to `/forms/auth/signup` or `/forms/auth/login`.  
- On success, the modal closes, and the header updates (via `loggedIn` event) to show the user menu.

---

## 5. Writer / Editor

### 5.1 New Post
- `GET /write` → full page with blank editor.  
- The post is **always associated with the user’s default blog**. Future enhancements can allow blog selection.

### 5.2 Edit Existing Draft/Post
- `GET /write/draft/{postId}` → pre‑fills the editor with existing data.  
- Only the post author can access this.

### 5.3 Saving
- **Save Draft:** `POST /forms/write/draft` (HTMX, no redirect, shows toast).  
- **Publish:** `POST /forms/write/publish` → sets `published = true`, redirects to the published post.

### 5.4 Editor Toolbar
- Format: Markdown / AsciiDoc switchable (reflected in `format` field).  
- Cover image upload via `/api/images`.  
- Slug, description, and content fields.  
- Real‑time preview toggle.

---

## 6. Library & Dashboard

### 6.1 Library (`GET /writing/library`)
- Tabs: **Drafts** and **Published**.  
- Tabs load their content dynamically via HTMX (`GET /writing/library/components/tab/drafts` or `published`).  
- Each entry shows title, metadata, and action buttons: **Edit** on both tabs; **Unpublish** on published rows; **Delete** on draft rows (including posts **unpublished** from the Published tab).  
- **Unpublish** (`POST /forms/posts/{postId}/unpublish`): blog owner only; sets `published = false`, clears featured; publication history is kept.  
- **Delete** (`DELETE /forms/posts/{postId}`): blog owner only; allowed only when `published = false` (unpublish first if currently published).  
- A user can only see and manage their own posts (filtered by blog ownership; server rejects foreign post ids with 404).

### 6.2 Dashboard (`GET /manage/dashboard`)
- Ownership: only authenticated users.  
- **Analytics:** Per selected blog (`?blogId=` optional; defaults to main blog). `GET /manage/dashboard/components/analytics` returns HTMX fragment with daily views (optional comparison to previous calendar month), daily reading time, new followers, and new email subscribers for the chosen month.  
- Displays counts and recent activity (drafts, published) across all user’s blogs.  
- Quick action to write.

---

## 7. Account security and author appearance

### 7.1 Account security (`GET /account/security`)
- Requires authentication.  
- Form to update email and password.  
- Password change requires current password; triggers a **password changed** account email.  
- Email change requires verification: sets **pending email**, sends link to new address; confirmed email stays active until `GET /account/verify-email?token=…`.  
- POST to `/forms/account/security` for email/password changes; POST to `/forms/writing/appearance` for display name and images.

### 7.2 Author appearance (`GET /writing/appearance`)
- Requires authentication.  
- Form to update display name, profile picture, and default blog banner.  
- Display name change requires current password; image-only saves do not.  
- POST to `/forms/writing/appearance`.

### 7.3 Author blogs (`GET /writing/blogs`)
- List, create, and edit own blogs (name, slug, banner).  
- **Edit** on the default blog includes description and **Git sync** (remote URL, branch, sync history).  
- **Settings** on a row opens the extended form for secondary blogs (description, active, Git).  
- Editors use **Manage** → **Blogs** for platform-wide list and deactivation.

---

## 8. Authentication

### 8.1 Sign-up / Sign-in modals
- Sign-up and sign-in via header modals (`GET /auth/modal?mode=signup|login`).  
- Sign-in includes **Forgot password?** linking to password recovery.

### 8.2 Password recovery
- **Request:** `GET /password-recovery` → POST `/forms/auth/password-recovery/request` (generic success; email sent only for active accounts).  
- **Reset:** link in email → `GET /password-recovery/reset?token=…` → POST `/forms/auth/password-recovery/reset`.  
- Successful reset invalidates all sessions and sends a **password changed** notice.

### 8.3 Account emails (access / user management)
| Email | Trigger |
|-------|---------|
| Activate your {siteName} account | Self-service sign-up (includes **Notify site administrator** link if the recipient did not create the account) |
| Unauthorized signup reported on {siteName} | Recipient clicks report link in activation email (`GET /account/report-signup?token=…`) |
| Reset your contraponto password | Password recovery request |
| Your contraponto password was changed | Reset, profile password change, admin sets new password |
| Confirm your new email address | Profile email change (to pending address) |
| Your contraponto email address was changed | After email verification (to previous address) |

---

## 9. Review Page (`GET /review`)
- **Access:** Users with `EDITOR` role only.  
- Lists all published posts across the platform.  
- Each row has a star toggle; clicking it sends a PUT to toggle the `featured` flag and updates the row via HTMX swap.  
- The endpoint that handles the toggle must be secured (e.g., `@RolesAllowed("EDITOR")`).

---

## 9. Custom Pages

- **Global pages:** `GET /page/{slug}` – served from global custom pages (blogOwner = null).  
- **Blog‑specific pages:** `GET /page/{username}/{slug}` – tied to a user’s blog.  
- Pages can have a placement (footer, sidebar, both, none). Footer links are dynamically loaded via `GET /_footer-pages` and inserted into the footer.  
- Editors manage custom pages via `/admin/custom-pages`.

---

## 10. Configuration & Admin

- `GET /config/` – reserved for user configuration (future).  
- All administrative endpoints (review, custom page management) should be under `/admin` or secured appropriately.

---

## 11. Subcomponents (HTMX partials)

- `GET /components/menu` – returns the user menu (or auth buttons) depending on authentication state.  
- `GET /components/write-btn` – returns the “Write”/“Publish”/“Save Draft” buttons if authenticated.  
- `GET /components/home/grid?page=X` – returns post grid fragment for infinite scroll.  
- `GET /components/header` – the site header (used internally by templates).

These partials are triggered by events (`loggedIn`, `loggedOut`) to keep the header in sync without a full page reload.

---

## 12. Error Pages

- **404:** `GET /error/404` (handled by a global error template).  
- **403:** Access denied (e.g., trying to visit `/review` without editor role).  
- **500:** Generic server error (with optional technical details in dev mode).

---

## 13. Authentication & Role Flow

- On login, the server sets an `__session` cookie and triggers `loggedIn`.  
- On logout, the cookie is cleared and `loggedOut` triggers.  
- The `LoggedUser` bean holds the current user’s details and roles.  
- Role checks:  
  - **Author:** matches post.blog.owner.id.  
  - **Editor:** role field equals `"EDITOR"`.  
- The `MenuEndpoint` uses `LoggedUser` to show/hide the Review link.

---

## 14. Important Design Decisions

- **Feature toggle** is immediate; no confirmation dialog.  
- **The default blog** is the single entry point for writing; secondary blogs must be created via an admin interface later.  
- **Pagination:** public reading lists use HTMX “Load more” (1‑based `?page=`); managing lists use numbered pagination with item totals (`components/manage-pagination.html`, page size 20).  
- **HTMX** is used for all dynamic updates (search, modal loading, infinite scroll, feature toggle).  
- **Static resources** (favicon, CSS, JS) are served from `META-INF/resources` and intercepted by a filter to avoid clashes with catch‑all paths.  
- **The `/{username}` path** now checks if the user has multiple blogs: if yes, it shows a blog list; otherwise it redirects internally to the default blog’s page.

---

*This document should be updated as new features are added. Keep the route map and flow descriptions consistent with the implementation.*