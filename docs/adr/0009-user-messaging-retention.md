# ADR-0009: User messaging — flagged-thread retention

## Status

Accepted

## Context

Contraponto is adding private **user messaging** ([feature/user-messaging.md](../../feature/user-messaging.md)). Participants may **flag thread as inappropriate**; only **`ADMIN`** users review reports. We need a default policy for how long flagged content and report metadata are retained and whether admin review affects participant-visible threads.

## Decision

1. **Report records** (`MessageReport`) are retained for **90 days** after creation, then eligible for scheduled purge (report row only; thread messages remain unless separately deleted by a future policy).
2. **Admin review** (`Dismissed`, `Reviewed`) does **not** delete or hide the thread for participants — it only closes the report queue item.
3. **Administrators** may read full thread content when reviewing a report; access is audited via report `reviewed_at` / `reviewed_by_user_id`.
4. Participant **block** and **freeze** behaviour is independent of report status.

## Consequences

### Positive

- Predictable storage and privacy posture for moderation data.
- Participants keep their mailbox history unless they close the thread or a block freezes it.

### Negative

- Flagged harassment content may remain visible to participants until they close the thread or block — mitigated by freeze-on-block (FQ7).
- Requires a future scheduled job or manual purge for old report rows.

## Changelog

| Data | Evento | Detalhe |
|------|--------|---------|
| 2026-07-07 | proposed | Rascunho inicial para feature user-messaging. |
| 2026-07-07 | accepted | Aceite manual do usuário. |
