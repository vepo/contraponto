# CDI events catalog

Cross-context side effects in Contraponto use Jakarta CDI `Event` + `@Observes`. Producers fire after the owning transaction commits business work (or `AFTER_SUCCESS` where noted). Observers must stay idempotent where possible (cache invalidation, notifications).

**Related:** [ARCHITECTURE.md](../ARCHITECTURE.md) §9, [domain-specification.md](domain-specification.md) §Bounded contexts.

## Publishing & content

| Event | Payload | Producer | Observers | Notes |
|-------|---------|----------|-----------|-------|
| `PostPublishedEvent` | `postId`, `publicationId`, `blogId`, `authorUserId` | `PostPublicationService.publish` | `PostPublishedNotificationObserver`, `RssFeedCacheInvalidator`, `SitemapCacheInvalidator`, `ActivityPubDeliveryObserver` | Also triggers audience email/in-app notifications; ActivityPub **Create** when Fediverse opt-in |
| `PostUnpublishedEvent` | `postId`, `blogId`, `authorUserId` | `PostManagementService.unpublish` | `RssFeedCacheInvalidator`, `SitemapCacheInvalidator`, `ActivityPubDeliveryObserver` | Public URL 404; snapshots retained; ActivityPub **Delete** when Fediverse opt-in |
| `PostGitSyncRequestedEvent` | `postId`, `GitSyncTrigger` | `PublishEndpoint`, `SaveDraftEndpoint`, `PostManagementService` | `GitPostCommittedObserver` (`AFTER_SUCCESS`) | Git export when blog has `gitEnabled` |
| `CustomPageChangedEvent` | `pageId` | `CustomPageSaveEndpoint` (create/update/delete) | `CustomPageCacheRefreshObserver`, `SitemapCacheInvalidator` | In-memory custom page cache |

## Auth email payloads (not CDI events)

Types under `auth/*Event` (`AccountActivationEvent`, `PasswordResetEvent`, etc.) are **email template payloads** passed to `AccountEmailService` / outbox — they are **not** fired on the CDI event bus.

## Framework events

| Event | Observer | Notes |
|-------|----------|-------|
| Quarkus `StartupEvent` | `DatabaseDevSetup`, `ActivityPubDevSeed` | Runs `%dev` import script; seeds `alice` ActivityPub actor when enabled |

## Guidelines

- **Prefer events** over direct calls from publishing into notification, RSS, SEO, Git, or ActivityPub when the reaction is a side effect of publish/unpublish/custom-page change.
- **Do not** inject repositories from another bounded context into a service observer unless the read is trivial and documented; prefer the event payload ids.
- New events: add a row here and in [domain-specification.md](domain-specification.md) if they introduce domain vocabulary.
