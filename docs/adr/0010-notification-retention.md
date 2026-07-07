# ADR-0010: In-app notification retention

## Status

Accepted

## Context

In-app **notifications** (`tb_notifications`) are stored indefinitely today: **Dismiss** and **mark all read** only set `read = true`; rows are never purged by age. The platform is adding **user messaging** notifications (`NEW_MESSAGE_THREAD`, `NEW_THREAD_MESSAGE`) and needs bounded storage and a clear privacy posture.

Product requirement: purge **read** notifications after a **few days**, and **unread** notifications after a **longer** period.

## Decision

1. Add nullable **`read_at`** on `tb_notifications`, set when a notification becomes read (single dismiss or mark-all-read).
2. **Scheduled purge** (daily) deletes rows past configurable age:
   - **Read:** `read = true` and `read_at` older than **`app.notifications.retention.read-days`** (default **7**).
   - **Unread:** `read = false` and `created_at` older than **`app.notifications.retention.unread-days`** (default **30**).
3. Purge is **hard delete** from `tb_notifications` only; it does not delete posts, blogs, or message threads.
4. Applies to **all** `NotificationType` values uniformly (including future message types).
5. **Backfill:** migration sets `read_at = created_at` for existing rows where `read = true` so legacy read items become eligible for purge on schedule.

## Consequences

### Positive

- Predictable storage; read items disappear from `/account/notifications` after the short window.
- Unread items stay longer so users who rarely open the bell still have time to see alerts.

### Negative

- Users cannot recover dismissed/read notifications after the read retention window.
- Unread notifications older than the unread window are deleted even if never seen — mitigated by 30-day default vs 7-day read window.

## Configuration

| Property | Default | Meaning |
|----------|---------|---------|
| `app.notifications.retention.read-days` | `7` | Delete read notifications when `read_at` is older than this many days |
| `app.notifications.retention.unread-days` | `30` | Delete unread notifications when `created_at` is older than this many days |
| `app.notifications.retention.schedule` | `24h` | Quarkus `@Scheduled` interval for purge job |

## Changelog

| Data | Evento | Detalhe |
|------|--------|---------|
| 2026-07-07 | proposed | Rascunho inicial — 7 dias lidas, 30 dias não lidas. |
| 2026-07-07 | accepted | Aceite manual do usuário. |
