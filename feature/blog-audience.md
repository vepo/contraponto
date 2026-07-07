# Blog audience (follow, subscribe, notifications delivery)

**Feature version:** 1  
**Status:** done  
**Production:** live

## Changelog

### Production baseline — 2026-07-07

**Version:** 1  
**Status:** done

**Production:** live — deployed capability

## Summary

Signed-in **readers** **follow** blogs and **subscribe by email** for new post alerts. **In-app notifications** (bell, overlay, Account hub) deliver platform events including `NEW_POST` on publish. Distinct from [notification-retention.md](notification-retention.md) (purge policy) and [user-messaging.md](user-messaging.md).

## Wireframe

| Screen | Route | Notes |
|--------|-------|-------|
| Audience widget | On blog/post pages | Follow / Subscribe buttons |
| Notification bell | Header | Badge poll + overlay |
| Notifications inbox | `GET /account/notifications` | Dismiss, mark all read |
| Subscriptions | `GET /account/subscriptions` | Followed blogs list |

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `notification` |
| Schema | `tb_blog_audience`, `tb_notifications`, `tb_email_notification_log` |
| CDI | `PostPublishedEvent` → `PostPublishedNotificationObserver` |
| Tests | `BlogAudienceEndpointTest`, `PostPublishedNotificationTest`, `FollowAfterLoginWebTest`, `NotificationOverlayWebTest` |



### Risks

| Risk | Mitigation |
|------|------------|
| Guest follow intent lost after login | User must click Follow again (FQ2) |
| Email dedup | `tb_email_notification_log` per publication+user |

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Follow own blog? | answered | **Rejected** — `BlogAudienceService` |
| FQ2 | Auto-follow after login from guest click? | open | Not implemented — manual re-click |
| FQ3 | Email on republish? | answered | Dedup by `publication_id`; identical republish skips event |

## Architecture

### ADRs aplicáveis

| ADR | Relevância |
|-----|------------|
| [0003](../docs/adr/0003-frontend-qute-htmx.md) | Audience + bell HTMX |
| [0010](../docs/adr/0010-notification-retention.md) | Retention (separate feature) |
| [0013](../docs/adr/0013-cdi-events-cross-context.md) | `PostPublishedEvent` observer |

### Design específico da feature

| Area | Design |
|------|--------|
| Service | `BlogAudienceService` — toggle follow/subscribe; delete row when both off |
| Notifications | `NotificationService` — create by type; link builders per type |
| Email | `PostNotificationEmailService` + mock mailer in dev |

### HTMX component model

| Component id | Route | Activator | Target/swap | Events out | Events in | JS |
|--------------|-------|-----------|-------------|------------|-----------|-----|
| `#blog-audience-{id}` | `GET /components/blogs/{id}/audience` | load, auth refresh | outerHTML self | OOB on POST | `loggedIn`/`loggedOut` | none |
| Follow/subscribe | `POST /forms/blogs/{id}/follow|subscribe` | click | `#blog-audience-{id}` | toast | — | none |
| `#notification-badge-container` | `GET /components/notifications/badge` | poll, `notificationsChanged` | inner | — | allowlist | `header.js` |
| `#notificationOverlay` | `GET /components/notifications/overlay` | bell open | innerHTML | — | `notificationsChanged` | `header.js` |
| Dismiss | `POST /forms/notifications/{id}/dismiss` | click | overlay refresh | `notificationsChanged` | — | none |

#### Feature checklist

| ID | Criterion | Done |
|----|-----------|------|
| FC1 | Follow / unfollow on blog/post | ☑ |
| FC2 | Email subscribe toggle | ☑ |
| FC3 | Bell badge + overlay | ☑ |
| FC4 | Account notifications + subscriptions hubs | ☑ |
| FCdev | `dave` follower, `eve` subscriber, sample notifications | ☑ |

**Development approval:** approved — production baseline (shipped)
**Review approval:** approved 2026-07-07 — production baseline
