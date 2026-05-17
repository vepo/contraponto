# Git + Jekyll layout convention

Contraponto mirrors each configured blog against a Git repository shaped like a [Jekyll](https://jekyllrb.com/) content tree. Paths and front matter keys follow **defaults** documented here so repositories work out of the box. You may override mappings with `_contraponto.yml` at the repository root.

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

Exported post bodies reference images under `assets_directory` (e.g. `assets/images/{uuid}.png`). Alt text is stored in Contraponto and written into Markdown `![alt](path)` on export. Optional front matter `cover` uses the same path style.

Changes apply immediately on the next import or export after a `git pull` / scheduler run.

## Server configuration (`application.properties`)

| Key | Meaning |
|-----|---------|
| `contraponto.git.workspace-root` | Where clones live; blank → `${java.io.tmpdir}/contraponto-git` |
| `contraponto.git.poll-enabled` | When `true`, the scheduler pulls + imports periodically |
| `contraponto.git.poll-interval` | Quarkus Scheduler duration (`2m`, `120s`, …) |
| `contraponto.git.username` / `contraponto.git.password` | Optional HTTPS credentials (often a PAT) |

SSH remotes instead of HTTPS typically rely on OS-level SSH configuration (outside this app).

## Flow summary

1. **Editor writes in Contraponto** — after each draft save or publish, the post is queued for export: pull, write Markdown under `_posts/` or `_drafts/`, commit, push when there are staged changes.

2. **Remote edits Git** — the scheduler clones/fetches periodically; markdown is parsed and merged into posts for that blog (`contraponto_post_id` or slug match).

Use a dedicated branch (`git_branch` on the blog) if you coexist with CI that builds GitHub Pages on `main`.
