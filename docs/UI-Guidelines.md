Contraponto

## 1. Core Philosophy

**“Readable like a newspaper, responsive like a modern web app.”**  
- **No page reloads** – HTMX swaps only the `<main>` or smaller fragments.  
- **Typographic focus** – elegant serif fonts for content, clean sans‑serif for UI.  
- **Editorial design** – generous whitespace, strong hierarchy, understated colors.  

---

## 2. Design Principles

| Principle          | How it’s applied                                                                 |
|--------------------|----------------------------------------------------------------------------------|
| **Clarity**        | High contrast text, intuitive navigation, clear call‑to‑actions.                  |
| **Consistency**    | Reuse components (cards, buttons, forms) – see `/components` and `/shared` CSS.   |
| **Progressive enhancement** | Core functionality works without JS; HTMX adds SPA‑like feel.                      |
| **Modularity**     | Each page is composed of interchangeable blocks (featured, grid, actions).         |
| **Performance**    | Minimal custom JavaScript; use HTMX for most interactions; cache static assets.    |

---

## 3. Component Inventory

### 3.1 Global Shell

- **Header** (`site-header` – sticky)  
  - Logo (links to `/`).  
  - Left: hamburger menu icon (future sidebar).  
  - Right: search button, write button (if authenticated), user menu (dropdown).  
  - Auth buttons (sign‑up / sign‑in) when not logged in.  
- **Footer** (`site-footer`)  
  - Logo, description.  
  - Static links (Archive, Topics, About…).  
  - Custom page links (loaded dynamically via HTMX from `/page/_footer-pages`).  
  - Toast container (`#toast`).  
- **Modal Container** (`#modal-container` – empty div at bottom of `<head>`)  
  - Hosts modals injected by HTMX (auth, search).  

### 3.2 Page Templates

| Page                | Key elements                                              | Related files                                                        |
|---------------------|-----------------------------------------------------------|----------------------------------------------------------------------|
| **Home**            | Featured article (large), 3‑column post grid, load‑more.  | `HomeEndpoint/home.html`, `featured.html`, `grid.html`               |
| **User Blog**       | Same layout, but filtered to a user’s posts.              | `UserBlogEndpoint/home.html`, `featured.html`, `grid.html`           |
| **Article**         | Cover image, title, author link, metadata, rich content.  | `PostEndpoint/post.html`                                             |
| **Write / Edit**    | Title, cover upload, slug, description, editor, toolbar, preview. | `WriteEndpoint/write.html`, `write.js`                        |
| **Profile**         | Account settings form (name, email, password).             | `ProfileEndpoint/profile.html`                                       |
| **Dashboard**       | Stats cards (drafts, published), recent activity, quick write link. | `DashboardEndpoint/dashboard.html`                         |
| **Library**         | Tabs for drafts / published, edit/delete buttons.         | `LibraryEndpoint/library.html`, `postsList.html`                     |
| **Search**          | Input with live results (modal or full page).             | `SearchEndpoint/modal.html`, `search.html`, `results.html`           |
| **Review**          | List of published posts with toggle featured button.       | `ReviewEndpoint/review.html`, `postRow.html` (planned)               |
| **Custom Page**     | Simple static page with title and rendered content.        | `CustomPageResource/page.html`                                       |
| **Error**           | Status code, explanation, back‑home / retry buttons.       | `templates/error.html`                                               |

### 3.3 Reusable UI Patterns

#### **Article Cards** (`.article-card`)
- Used in grids (home, user blog, search results).  
- Contains: image, category, title (link), excerpt, meta (author, date, read time).  
- Hover effect: lift shadow, image scale.  

#### **Featured Section** (`.featured`)
- Two‑column layout (text + image).  
- Category label, large title, excerpt, meta.  

#### **Buttons**
- `.btn` – base style, rounded, transition border/background.  
- `.btn--primary` – green, for main actions.  
- `.btn--small` – compact for lists.  
- `.btn--icon` – with SVG icon for edit/feature.  
- `.btn--outline` – transparent border, used in actions bar.  
- `.btn--star-active` – yellow star for featured toggle.  

#### **Form Elements**
- `.form-group` – label + input with error messages below.  
- Inputs: focus border turns green, validation errors shown conditionally.  
- Submit button disabled until form valid.  

#### **Error / Success Messages**
- `.error-message` – hidden by default, shown via JS when validation fails.  
- `.success-message` – green background, used after profile save etc.  

#### **Toast Notifications** (`.toast`)
- Fixed bottom‑right, slides in, auto‑hides.  
- Can be triggered by response header `X-Toast-Message`.  
- Supports types: success (green), error (red).  

#### **Modal** (`.modal`)
- Centered box, backdrop, open/close transitions.  
- Used for auth forms and search.  

#### **Loading States**
- `.loading-spinner` – placeholder while HTMX request is in‑flight.  
- `.htmx-indicator` – class added to indicate activity (global CSS).  

---

## 4. HTMX Interaction Patterns

- **Navigation** – all internal links use `data-hx-get` + `hx-select="main"` + `hx-target="main"` + `hx-push-url`. This replaces only the main content area and updates the URL without full reload.  
- **Forms** – use `hx-post` or `hx-put` with `hx-target` for error/success areas.  
- **Live Search** – input triggers `keyup changed delay:300ms` with `hx-get`; explicit button also fires `hx-get`.  
- **Lazy loading** – footer custom pages use `hx-get` with `hx-trigger="load"`.  
- **Tabs** (Library) – buttons with `hx-get="/library/tab?type=…"`, `hx-target="#libraryContent"`.  
- **Pagination** – “Load more” button that fetches the next grid page and swaps itself out.  

**Rule of thumb:** Any action that should not cause a full page refresh is a candidate for HTMX.

---

## 5. Theming & CSS Variables

All styling lives in `main.css` and `write.css`.  
**CSS custom properties** are defined in `:root` of `main.css`:

| Variable               | Purpose                     | Default                |
|------------------------|-----------------------------|------------------------|
| `--color-primary`      | Main action color (links, buttons) | `#1a8917` (green) |
| `--color-accent`       | Drop caps, decorative lines | `#c41e3a` (red)       |
| `--color-text`         | Primary text                | `#1a1a1a`             |
| `--color-text-light`   | Excerpts, secondary text    | `#4a4a4a`             |
| `--color-text-muted`   | Metadata, footnotes         | `#6b6b6b`             |
| `--color-border`       | Borders, dividers           | `#e5e5e5`             |
| `--color-bg`           | Background                  | `#ffffff`             |
| `--color-bg-offset`    | Off‑white background        | `#fafafa`             |
| `--font-serif`         | Body text (Cormorant Garamond) |                      |
| `--font-serif-display` | Headings (Playfair Display) |                      |
| `--font-sans`          | UI, meta (Inter)            |                      |

**Do not override these variables** – use them in new components to stay consistent.

---

## 6. Responsive Breakpoints

| Breakpoint | Target         | Layout changes                                   |
|------------|----------------|--------------------------------------------------|
| ≥ 1024px   | Desktop        | 3‑column grid, two‑column featured.              |
| 768‑1023px | Tablet         | 2‑column grid, stacked featured, compact header. |
| ≤ 767px    | Mobile         | 1‑column grid, stacked featured, full‑width cards, reduced header. |
| ≤ 480px    | Small mobile   | Further reduced padding, simplified typography.  |

**General rules:**  
- Use `max-width: var(--container-max)` for content.  
- Use `container-narrow` for reading‑centered pages (articles, profiles, settings).  
- Images maintain aspect ratio with `object-fit: cover`.  

---

## 7. Security & Role‑Based UI

- **Editors/Admins** see additional menu items (Review, Custom Pages management).  
- **Post author** sees “Edit” button.  
- **Editors** see “Toggle Featured” on post actions bar.  
- Non‑authenticated users see Sign‑Up / Sign‑In buttons.  
- Always check roles server‑side; UI hiding is only cosmetic.  

---

## 8. File Naming & Organization

- **Templates** – `templates/EndpointName/page.html`.  
- **JavaScript** – `js/main.js`, `js/forms.js`, `js/write.js`, `js/header.js`, etc. Each has a single class.  
- **CSS** – `main.css` for global styles, `write.css` for editor‑specific styles.  

---

## 9. Development Workflow

- Use Qute `{#include ...}` to compose pages (head, header, footer).  
- For new HTMX interactions, add minimal JavaScript only if absolutely necessary (see `js/` files for examples).  
- Always test that a page works without JavaScript by disabling JS – the core navigation should still function (HTMX relies on JS, but progressive enhancement should keep basic links/forms operational).  

---

This guideline ensures that any new feature or page blends seamlessly into the existing design language and technical architecture. Adherence to it results in a cohesive, high‑quality reading and writing experience.