# Post publication versioning (immutable snapshots)

> **Status**: Proposed
>
> **Updated**: 2026-07-07
>
> **Aceitação / reabertura:** somente **manual** pelo usuário humano.

## Summary

Published posts expose an immutable **publication snapshot** (`PostPublication` in `tb_post_publications`) while the working copy (`Post` in `tb_posts`) remains editable. **Republish** creates a new snapshot when content diverges; **unpublish** returns to draft without deleting history. Slug changes register **slug aliases** for redirects.

## Drivers

* Readers must see stable content at a URL while authors edit drafts.
* Version history and diffs require retained snapshots.
* Republish must not spam notifications when content is identical (domain invariant).

## Options

### Working copy + immutable snapshots (implemented)

`Post.livePublication` points to current public snapshot; `PostPublicationService.publish` manages versions.

### Single row overwrite on publish

No version history; simpler but loses audit trail.

### External CMS as source of truth

Out of scope.

## Options Analysis

### Working copy + immutable snapshots

* Pro: Version history UI; Git export per publication; notification dedup by `publication_id`.
* Con: Schema complexity; tag/image dependencies duplicated per snapshot.

## Recommendation

Keep **working copy + `PostPublication` snapshots** with `PostChangeDiffService` for history modal.

### Exception — legacy link reprocess (v3, planned; [feature/git-sync.md](../../feature/git-sync.md))

Git sync's **legacy link reprocess** (rewriting self-links matched by a blog's **legacy link pattern**) is the **one sanctioned exception** to snapshot immutability: it may update `PostPublication.content` **in place** for existing snapshots, not just the working copy. Scope of the exception is narrow and mechanical:

* Limited to substituting matched **legacy link** URL occurrences with the target post's canonical internal URL — never any other content change.
* Deterministic and idempotent — the same pattern applied to the same post state always produces the same output.
* Does **not** create a new `PostPublication` version, does not change `publishedAt` / `livePublication`, and does not fire `PostPublishedEvent` (no notification/email/ActivityPub side effects) — it is corrective rewriting of existing content, not new authored content.

This exists so readers see corrected internal links immediately after an author fixes their **legacy link pattern**, without an artificial republish cluttering version history (domain-specification invariant 50).

### Consequences

* Pro: Aligns with Git Jekyll export (`published_at` per publication).
* Con: Storage grows with republish frequency; snapshots not purged on unpublish.

### Confirmation

* `PostPublicationServiceTest`, `PostChangeHistoryTest` green.
* `dev-import.sql` includes multi-version examples.

## Changelog

| Data | Evento | Detalhe |
|------|--------|---------|
| 2026-07-07 | proposed | Retroactive ADR documenting shipped publish model. |
| 2026-08-05 | proposed | Added narrow exception for Git sync's legacy link reprocess (in-place snapshot content rewrite, no new version) — see [feature/git-sync.md](../../feature/git-sync.md) v3. |
| 2026-08-05 | accepted | User accepted the legacy-link-reprocess exception specifically. Overall ADR status remains **Proposed** — the original 2026-07-07 baseline recommendation has not itself been put to the user for acceptance; only the 2026-08-05 exception was. |

## More Information

* [feature/post-publishing.md](../../feature/post-publishing.md)
* Domain spec § Posts & publishing
