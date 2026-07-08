# ADR-0016: Account deletion — abuse audit retention

> **Status**: Accepted
>
> **Updated**: 2026-07-08
>
> **Aceitação / reabertura:** somente **manual** pelo usuário humano.

## Summary

When a user **self-deletes** their account, Contraponto removes public profile and owned content but retains **admin-only audit data** (identity snapshot, client IP timeline, post/comment content snapshots) for **6 months**, then purges the snapshot and all related audit events for that account.

## Drivers

* Abusers may register, publish harmful content, and self-delete to evade public attribution.
* Operators need a bounded forensic trail without indefinite storage of personal data.
* Product answers **FQ10** and **AQ1** in [feature/account-deletion.md](../../feature/account-deletion.md).

## Options

### Six months, purge snapshot + all audit events

`purge_after = deleted_at + 6 months`. Scheduled job deletes `tb_account_deletion_snapshots` row and every `tb_user_audit_events` row for the same stable account subject id (`former_user_id` / `account_subject_id`).

### Twenty-four months, snapshot-only purge

Longer retention; purge only the deletion snapshot; leave IP login events orphaned.

### No retention (hard erasure)

Full delete with no audit — rejected for abuse investigation.

## Options Analysis

### Six months, purge snapshot + all audit events

* Pro: Matches product choice; predictable privacy posture; single purge unit per deleted account.
* Con: Shorter window than some legal holds; repeat offenders who wait 6 months leave no platform trail.

### Twenty-four months

* Pro: Longer abuse investigations.
* Con: More PII stored; user chose 6 months.

## Recommendation

Adopt **six months** retention and **purge snapshot + all audit events** for the same `former_user_id` when `purge_after` elapses.

Implementation notes:

1. **`tb_account_deletion_snapshots.purge_after`** = `deleted_at + 6 months` (configurable via `app.account-deletion.audit.retention-months`, default **6**).
2. **`tb_user_audit_events`** carries a stable **`account_subject_id`** (the original `tb_users.id`) on every event so rows remain queryable after `user_id` FK is set null on account delete.
3. **Daily scheduler** (`AccountDeletionAuditRetentionScheduler`) deletes eligible snapshots and matching audit events in one transaction per snapshot.
4. Audit data is **`ADMIN`**-only; not exposed on the public site or to the deleted user.

## Consequences

### Positive

* Operators can investigate harmful content after self-delete within a defined window.
* Storage and privacy exposure are bounded; aligns with LGPD data-minimization when disclosed in privacy copy.

### Negative

* Accounts deleted before this feature ships have no historical IP events (only delete-time snapshot once live).
* Six months may be insufficient for slow legal processes — operators rely on external logs if needed.

### Confirmation

* Feature **FC9**: scheduler + config property; unit test on purge selection.
* Integration test: delete account → snapshot exists → advance clock / set `purge_after` in past → job removes snapshot and events.

## Changelog

| Data | Evento | Detalhe |
|------|--------|---------|
| 2026-07-08 | proposed | FQ10 = 6 months; AQ1 = purge snapshot + all audit events for `former_user_id`. |
| 2026-07-08 | accepted | Aceite manual do usuário ("Approve"). |

## More Information

* [feature/account-deletion.md](../../feature/account-deletion.md)
* [ADR-0009](0009-user-messaging-retention.md) — 90-day message **report** retention (separate concern)
* [ADR-0010](0010-notification-retention.md) — in-app notification purge pattern (scheduler precedent)
