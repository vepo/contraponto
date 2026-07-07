# Notification retention

**Feature version:** 1  
**Status:** done  
**Requested:** 2026-07-07

## Summary

Introduce a **notification retention policy** for in-app **notifications**: automatically purge rows from `tb_notifications` after configurable age. **Read** notifications are deleted sooner (few days after **read at**); **unread** notifications are kept longer (more days from **created at**). Replaces the current indefinite storage model.

Applies to all notification types, including future **user messaging** alerts ([user-messaging.md](user-messaging.md)).

## Wireframe

N/A — background job only; no new UI. Existing **Notifications** hub and bell behaviour unchanged until rows are purged.

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | `notification` |
| Schema | `tb_notifications.read_at`; migration backfill for existing `read = true` |
| Config | `app.notifications.retention.read-days`, `unread-days`, `schedule` |
| Code | `NotificationRepository.markRead` / `markAllRead` set `read_at`; `NotificationRetentionScheduler` + service |
| Tests | Unit (purge predicates, read_at on dismiss); integration (scheduler or service with fixed clock) |
| Docs | domain-spec, [ADR-0010](../docs/adr/0010-notification-retention.md), deployment.md |

### Risks

| Risk | Mitigation |
|------|------------|
| User expects old read list | 7-day default is generous for a dismiss-only inbox; document in domain spec |
| Unread lost without user seeing | 30-day unread window >> 7-day read window |
| Backfill sets `read_at = created_at` | Old read rows purge on next schedule after migration — acceptable |

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Retention day counts? | answered | **7 days** after read (`read_at`); **30 days** while unread (`created_at`). Configurable via `application.properties`. |
| FQ2 | Apply to all notification types? | answered | **Yes** — uniform policy. |
| FQ3 | Soft-delete vs hard-delete? | answered | **Hard delete** row from `tb_notifications`. |

## Architecture

### ADRs aplicáveis

| ADR | Relevância |
|-----|------------|
| [0002](../docs/adr/0002-backend-java-quarkus-jakarta-ee.md) | Quarkus scheduler |
| [0005](../docs/adr/0005-postgresql-database.md) | Flyway migration |
| [0010](../docs/adr/0010-notification-retention.md) | **Accepted** — retention policy |

### Design específico

| Area | Design |
|------|--------|
| **Schema** | `ALTER TABLE tb_notifications ADD COLUMN read_at TIMESTAMP(6)`; backfill `UPDATE … SET read_at = created_at WHERE read = true` |
| **Entity** | `Notification.readAt` (`LocalDateTime`, null until read) |
| **Write path** | `NotificationRepository.markRead`, `markAllRead` → set `read = true` and `read_at = now()` (only if not already read) |
| **Purge** | `NotificationRetentionService.purgeExpired()` — two bulk `DELETE` queries (read branch, unread branch) |
| **Scheduler** | `NotificationRetentionScheduler` — `@Scheduled(every = "${app.notifications.retention.schedule}", concurrentExecution = SKIP)` |
| **Defaults** | read **7**d, unread **30**d, schedule **24h** |
| **Observability** | `INFO` log line with counts deleted per run |

### Architecture questions (AQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | Track `read_at` vs infer from `created_at` for read rows? | answered | **`read_at` column** — required for “days after read”. |
| AQ2 | Single DELETE with OR vs two passes? | answered | Two parameterized bulk deletes for clarity and index use (`recipient_user_id, read, created_at` / `read_at`). |

## Changelog

### Notification retention — 2026-07-07

**Version:** 1  
**Status:** done

**Description:** Daily purge of read notifications after 7 days and unread after 30 days; `read_at` column; configurable properties.

**Recommended implementation order:** Ship **before** [user-messaging.md](user-messaging.md) (messaging notifications inherit retention immediately).

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| User messaging | `NEW_MESSAGE_*` notifications follow same retention |
| Platform insights | Daily notification counts use live table — historical counts not retained after purge |
| All notification producers | No change to create path |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | `read_at` set on dismiss and mark-all-read | AQ1 | ☑ |
| FC2 | Read notifications purged after 7 days (configurable) | FQ1 | ☑ |
| FC3 | Unread notifications purged after 30 days (configurable) | FQ1 | ☑ |
| FC4 | Migration backfills `read_at` for existing read rows | — | ☑ |
| FC5 | Daily scheduler with SKIP concurrent execution | — | ☑ |
| FC6 | Retention properties documented in `docs/deployment.md` | — | ☑ |
| FCdev | No seed change required | — | ☑ |

#### Tasks (phase 3)

| ID | Task | Done |
|----|------|------|
| T1 | Flyway `V0.0.10__notification_read_at.sql`: add `read_at`, backfill existing `read = true` | ☑ |
| T2 | `Notification.readAt` + `markRead` / `markAllRead` set `read_at` when transitioning to read | ☑ |
| T3 | `NotificationRetentionService.purgeExpired()` — bulk delete read (by `read_at`) and unread (by `created_at`) | ☑ |
| T4 | `NotificationRetentionScheduler` + `application.properties` defaults (`read-days=7`, `unread-days=30`, `schedule=24h`) | ☑ |
| T5 | `NotificationRetentionServiceTest` — boundary dates, counts, config overrides | ☑ |
| T6 | Extend dismiss / mark-all-read tests assert `read_at` populated | ☑ |
| T7 | Update `docs/deployment.md` with retention properties | ☑ |

#### Test coverage (phase 3)

| ID | Test | Covers | Done |
|----|------|--------|------|
| TC1 | `NotificationRetentionServiceTest` read purge boundary | T3, T5 | ☑ |
| TC2 | `NotificationRetentionServiceTest` unread purge boundary | T3, T5 | ☑ |
| TC3 | `NotificationEndpointTest` or `DismissNotificationEndpointTest` — `read_at` on dismiss | T2, T6 | ☑ |
| TC4 | `MarkNotificationsReadEndpointTest` — `read_at` on mark all | T2, T6 | ☑ |

**Development approval:** approved 2026-07-07 — tasks: T1–T7

**Implementation notes:** `V0.0.10__notification_read_at.sql`; `NotificationRetentionService` + scheduler; tests green.
