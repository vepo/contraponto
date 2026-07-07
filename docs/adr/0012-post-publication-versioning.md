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

## More Information

* [feature/post-publishing.md](../../feature/post-publishing.md)
* Domain spec § Posts & publishing
