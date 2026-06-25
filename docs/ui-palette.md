# UI palette guide

Canonical reference for **design tokens** (`:root` in [main.css](../src/main/resources/META-INF/resources/style/main.css)). For CSS class names, see [ui-elements.md](ui-elements.md). For UX flows, see [ui-guidelines.md](ui-guidelines.md).

**Before adding or changing UI styles:** read this document and use existing tokens. Do not add raw hex colors in `main.css` or `manage.css` — extend `:root` here first, then reference `var(--…)`.

**How to preview:** run `./mvnw quarkus:dev` and spot-check home, a post, header modals, Manage hub, and write toolbar.

---

## Surfaces

| Token | Hex | Usage |
|-------|-----|--------|
| `--color-bg` | `#f6f5f2` | Page background (warm paper) |
| `--color-bg-offset` | `#ebeae6` | Footer, subtle panels, hover fills |
| `--color-bg-elevated` | `#fdfcfa` | Cards, modals, dropdowns |
| `--color-bg-subtle` | `var(--color-bg-offset)` | Alias for offset surfaces |
| `--color-surface-muted` | `#f0efeb` | Inset panels (serie nav, code areas) |
| `--header-bg` | `rgba(246, 245, 242, 0.96)` | Sticky site header |

## Text

| Token | Hex | Usage |
|-------|-----|--------|
| `--color-text` | `#1a1a18` | Primary body and headings |
| `--color-text-light` | `#3a3a37` | Secondary copy |
| `--color-text-muted` | `#5a5a56` | Metadata, hints, footer legal |
| `--color-text-secondary` | `var(--color-text-light)` | Alias |

## Borders

| Token | Hex | Usage |
|-------|-----|--------|
| `--color-border` | `#d2d1cc` | Default dividers, input borders |
| `--color-border-strong` | `#b8b7b2` | Emphasized borders |

## Brand

| Token | Hex | Usage |
|-------|-----|--------|
| `--color-primary` | `#3a6b3e` | Links, primary buttons, focus accents |
| `--color-primary-dark` | `#2d5530` | Primary hover / pressed |
| `--color-primary-hover` | `#325f36` | Primary hover (buttons) |
| `--color-accent` | `#7a3344` | Drop caps, pull quotes, destructive emphasis |
| `--color-secondary` | `var(--color-accent)` | Alias |
| `--color-secondary-hover` | `#65303d` | Accent hover |
| `--color-on-primary` | `#f6f5f2` | Text on filled primary buttons |

## Semantic

| Token | Hex | Usage |
|-------|-----|--------|
| `--color-success` | `#2d5c40` | Success toast, positive states |
| `--color-success-muted` | `rgba(45, 92, 64, 0.12)` | Success backgrounds |
| `--color-danger` | `#8b3030` | Errors, danger buttons |
| `--color-danger-hover` | `#703030` | Danger hover |
| `--color-danger-muted` | `rgba(139, 48, 48, 0.1)` | Error field backgrounds |
| `--color-warning` | `#7a5a20` | Warnings |
| `--color-warning-muted` | `rgba(122, 90, 32, 0.12)` | Warning backgrounds |
| `--color-info` | `#3d5a7a` | Informational hints |
| `--color-info-muted` | `rgba(61, 90, 122, 0.1)` | Info backgrounds |
| `--color-code-inline` | `var(--color-info)` | Inline `code` in article body; listing block left accent |
| `--color-code-block-bg` | `var(--color-bg-offset)` | Fenced / listing code panel interior |
| `--color-disabled` | `#8a8a86` | Disabled submit buttons |

## Typography

| Token | Value | Usage |
|-------|-------|--------|
| `--font-serif` | Cormorant Garamond stack | Article body (default `body`) |
| `--font-serif-display` | Playfair Display stack | Titles, logo |
| `--font-sans` | Inter stack | UI chrome, forms, header |
| `--font-mono` | ui-monospace stack | Code |
| `body` font-size | `16px` | Global base (set in main.css) |
| `--text-xs` | `0.7rem` | Badges, fine print |
| `--text-sm` | `0.8125rem` | Buttons, labels |
| `--text-base` | `0.9375rem` | UI default |
| `--text-lg` | `1.125rem` | Section subtitles |
| `--text-xl` | `1.3125rem` | Panel titles |
| Article title (`.article-page__title`) | `2.25rem` desktop | Post page — not a token |

## Spacing

| Token | Value |
|-------|-------|
| `--spacing-2xs` / `--space-2xs` | `0.25rem` |
| `--spacing-xs` / `--space-xs` | `0.375rem` |
| `--spacing-sm` / `--space-sm` | `0.75rem` |
| `--spacing-md` / `--space-md` | `1.125rem` |
| `--spacing-lg` / `--space-lg` | `1.5rem` |
| `--spacing-xl` / `--space-xl` | `2.25rem` |
| `--spacing-2xl` / `--space-2xl` | `3rem` |

`--space-*` aliases map to `--spacing-*` (see `:root` in main.css).

## Layout

| Token | Value | Usage |
|-------|-------|--------|
| `--container-max` | `1080px` | `.container` |
| `--container-narrow` | `760px` | `.container-narrow` |
| `--layout-header-height` | `56px` | Site header |
| `--browse-rail-top` | `5rem` | Fixed top for browse margin rails (header + spacing below) |
| `--browse-rail-scroll-inset` | `6rem` | Vertical inset for rail `max-height` (`100dvh` minus header + bottom gap) |
| `--layout-container-padding` | `1.125rem` | Horizontal container padding |
| `--layout-page-padding` | `var(--spacing-lg)` | Section vertical rhythm |
| `--bp-sm` | `480px` | Phone landscape / small phones (use in `@media (min-width: 480px)`) |
| `--bp-md` | `768px` | Tablet / primary mobile boundary |
| `--bp-lg` | `1024px` | Desktop grid enhancements |
| `--bp-xl` | `1280px` | Margin rails, related-posts aside |
| `--tap-target-min` | `44px` | Minimum hit area for icons, tabs, toolbar buttons |

## Radius, shadow, motion

| Token | Value |
|-------|-------|
| `--radius-sm` | `4px` |
| `--radius-md` / `--border-radius` | `6px` |
| `--radius-lg` | `10px` |
| `--radius-pill` | `24px` |
| `--shadow-xs` … `--shadow-lg` | See main.css |
| `--duration-fast` | `120ms` |
| `--duration-normal` | `200ms` |
| `--duration-slow` | `280ms` |
| `--ease-out`, `--ease-in-out` | Cubic-bezier curves |
| `--focus-ring-color` | `rgba(58, 107, 62, 0.35)` |

## Component mapping

| Component | Tokens / classes |
|-----------|------------------|
| Primary action | `.btn--primary` → `--color-primary`, `--color-on-primary` |
| Secondary / neutral | `.btn--secondary`, `.btn--outline` → `--color-border`, `--color-text` |
| Destructive | `.btn--danger` → `--color-danger` |
| Compact actions | `.btn--small` |
| Header Write | `.btn--header` |
| Page shell | `.container`, `--color-bg` |
| Sticky header | `.site-header`, `--header-bg` |
| Success / error toast | `.toast--success`, `.toast--error` |

## Do / don’t

- **Do** use `var(--color-*)`, `var(--spacing-*)`, `var(--text-*)` in new rules.
- **Do** add missing semantics to this file and `:root` together.
- **Don’t** paste hex in `manage.css` — use tokens from main.css.
- **Don’t** introduce one-off grays; map to `--color-text-*` or `--color-border*`.
- **write.css** may still contain legacy hex; prefer tokens when editing that file.
