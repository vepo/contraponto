# Git + Jekyll layout convention

Contraponto mirrors each configured blog against a Git repository shaped like a [Jekyll](https://jekyllrb.com/) content tree. Paths and front matter keys follow **defaults** documented here so repositories work out of the box. You may override mappings with `_contraponto.yml` at the repository root.

Enable Git sync on the blog **Edit** form (default/main blog at `/{username}`) or under **Settings** for any blog. This is a common setup for a personal site repo (e.g. GitHub Pages at `username.github.io`).

## Defaults (convention)

| Aspect | Convention |
|--------|-------------|
| Config file | `_contraponto.yml` next to `_config.yml` (optional) |
| Published posts folder | `_posts/` |
| Draft posts folder | `_drafts/` |
| Published filenames | `{yyyy-MM-dd}-{slug}.md` (or `.markdown`) |
| Draft filenames | `_drafts/{slug}.md` |
| Markdown body | Jekyll YAML front matter + blank line + Markdown or AsciiDoc body |
| Stable post id field | YAML key `contraponto_post_id` (written by Contraponto; avoids ambiguity when titles/slugs change) |
| Layout | Front matter uses `layout: post` (configurable keys below) |

### Front matter Contraponto reads and writes

- `contraponto_post_id` — database id used for deterministic updates.
- `slug` — URL slug; inferred from filenames when omitted in `_posts/` / `_drafts/`.
- `title`, `description`, `format` (`MARKDOWN` or `ASCIIDOC`).
- `tags` — YAML list of label strings `["Tag A", "Tag B"]` or a comma-separated string.
- `serie` — optional series title (creates/links the serie in Contraponto).
- `featured` — boolean.
- `published` — boolean; overrides folder inference (`_posts/` defaults to published, `_drafts/` to draft).
- `published_at` — ISO-8601 instant or local date/time.

### Legacy Jekyll aliases (import only)

On **import**, Contraponto also accepts common Jekyll keys. Native keys above take precedence when both are set.

| Legacy key | Maps to | Notes |
|------------|---------|--------|
| `permalink` | slug | Last URL segment (e.g. `/posts/my-post` → `my-post`) |
| `image` | cover | Same path rules as `cover` |
| `publish_date` | `published_at` | Supports Jekyll datetimes such as `2023-09-26 15:39:23 +0300` |
| `published` | published vs draft | `false` in `_posts/` still imports as a draft |
| `series` | serie (series title) | Same as `serie`; posts with the same title share one serie, ordered by publish date |

**Description length** — post excerpts are stored in `VARCHAR(512)` on both the working copy and each **published snapshot**. On import and publish, longer `description` values are truncated; Git sync logs a warning when truncation happens. Shorten the excerpt in Git or in Contraponto to stay within 512 characters.

**Jekyll datetime filenames** — files named `YYYY-MM-DD-HH-MM-SS-slug.md` are common. Contraponto only parses `YYYY-MM-DD` from the filename; set `slug` or `permalink` in front matter so the URL slug is correct.

**Format** — front matter `format` if valid; otherwise inferred from the file extension (`.md` → `MARKDOWN`, `.adoc` / `.asciidoc` → `ASCIIDOC`).

**Assets** — image paths may include subdirectories under `assets_directory` (e.g. `assets/images/capas/photo.webp`).

**AsciiDoc block images** — import and export preserve the Jekyll/AsciiDoc block-title form when used:

```asciidoc
.Image caption shown below the figure
image::relative/path.png[]
```

An optional attribute list may appear between the title and the macro (e.g. `[#img-id,link=…]`). The first positional attribute inside `image::path[…]` is **alt text**, not the block title; do not replace `.Caption` lines with bracket captions on import. Contraponto stores `<!-- contraponto:image uuid="…" -->` immediately before each `image::` line and strips those markers when rendering published posts.

Export still writes Contraponto-native keys (`slug`, `cover`, `published_at`, `contraponto_post_id`).

## Overrides in `_contraponto.yml`

All keys optional; unspecified keys fall back to the defaults above.

```yaml
# Directory names relative to repo root (no traversal segments like "..")

posts_directory: "_posts"

drafts_directory: "_drafts"

# YAML key representing the Jekyll layout field in markdown front matter
layout_fm_key: "layout"

# Default value for layout on exported posts

default_layout: "post"

# Image assets synced with posts (relative to repo root)

assets_directory: "assets/images"

```

Exported post bodies reference images under `assets_directory`. When an image was imported from Git, Contraponto stores its **Git asset path** (e.g. `capas/photo.webp` under `assets/images/`) and uses that path on export; editor-uploaded images without a Git path export as `assets/images/{uuid}.ext`. Alt text is stored in Contraponto and written into Markdown `![alt](path)` on export. Optional front matter `cover` uses the same path style. Legacy Jekyll asset file names longer than a UUID are mapped to a deterministic UUID on import; Contraponto post content still references `/api/images/{uuid}.ext`.

Changes apply immediately on the next import or export after a `git pull` / scheduler run.

## Server configuration (`application.properties`)

| Key | Meaning |
|-----|---------|
| `contraponto.git.workspace-root` | Where clones live; blank → `${java.io.tmpdir}/contraponto-git` |
| `contraponto.git.poll-enabled` | When `true`, the scheduler pulls + imports periodically |
| `contraponto.git.poll-interval` | Quarkus Scheduler duration (`2m`, `120s`, …) |
| `contraponto.git.username` / `contraponto.git.password` | Optional HTTPS credentials (often a PAT) |
| `contraponto.git.clone-depth` | Shallow clone/fetch depth (`1` = latest commit only; `0` = full history) |

SSH remotes instead of HTTPS typically rely on OS-level SSH configuration (outside this app).

## Flow summary

1. **Editor writes in Contraponto** — after each draft save or publish, the post is queued for export: pull, write Markdown under `_posts/` or `_drafts/`, commit, push when there are staged changes.

2. **Remote edits Git** — the scheduler clones/fetches periodically; markdown is parsed and merged into posts for that blog (`contraponto_post_id` or slug match).

Use a dedicated branch (`git_branch` on the blog) if you coexist with CI that builds GitHub Pages on `main`.
