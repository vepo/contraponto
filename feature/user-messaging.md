# User messaging

**Feature version:** 1  
**Status:** done  
**Production:** live

## Changelog

### Production baseline — User messaging — 2026-07-07

**Version:** 1  
**Status:** done

**Production:** live — deployed capability

**Description:** Private 1:1 titled threads in Account hub; reply, close, flag (admin review), block and freeze; in-app notifications only.

**Recommended implementation order:** After [notification-retention.md](notification-retention.md) (T1–T7); messaging migration `V0.0.11__user_messaging.sql` follows `V0.0.10`.

**Impact on other features:**

| Feature / area | Impact |
|----------------|--------|
| Notifications | New types + schema FK; observer in `notification` package; subject to [ADR-0010](../docs/adr/0010-notification-retention.md) |
| Account hub | New nav sections Messages, Blocked users |
| Administration | New Message reports section (`ADMIN`) |
| Author profile | **Message** CTA when signed in |
| ActivityPub | None — federated inbox unchanged |

## Summary

Private **user-to-user messaging**: each **user** has a **mailbox** of **message threads** (titled conversations with exactly two participants). Participants can **reply**, **close thread**, **flag thread as inappropriate**, and **block user**. Blocking **freezes** affected threads and shows **User is blocked** to both sides. New messages surface as **in-app notifications** only (no email). **Administrator** users review flagged threads in Administration.

Distinct from **notifications** (platform event alerts), **post comments** (public), and **ActivityPub inbox** (federated S2S).

## Wireframe

**Source:** ASCII below  
**Last updated:** 2026-07-07



### Screen: Account hub — Messages (`GET /account/messages`)

| Region | Elements | Notes |
|--------|----------|-------|
| Hub nav | **Messages** under Activity group | Alongside Notifications, Subscriptions |
| Tabs | **Open** / **Closed** | Manage pagination per tab |
| List row | Thread title, other participant, preview, unread indicator | Library-style rows |
| Primary action | **Compose** | Opens compose panel |

```
┌─ Account ─ Messages ─────────────────────────────┐
│ [ Open ] [ Closed ]                    [ Compose ] │
├──────────────────────────────────────────────────┤
│ ● Re: collaboration     alice → you    2h ago      │
│   Would you be interested in…                    │
├──────────────────────────────────────────────────┤
│   Question about series  you → bob     yesterday   │
└──────────────────────────────────────────────────┘
```

### Screen: Thread (`GET /account/messages/{threadId}`)

| Region | Elements | Notes |
|--------|----------|-------|
| Header | Thread title, participant name | Breadcrumb via Account hub |
| Banner (frozen) | **User is blocked** | Both participants when block active |
| Messages | Chronological **thread messages** | Author + timestamp |
| Actions | **Reply**, **Close thread**, **Flag thread**, **Block user** | Confirm modal for destructive actions |
| Reply form | Textarea + **Send reply** | Hidden when closed or frozen |

### Screen: Compose (`GET /account/messages/compose`)

| Region | Elements | Notes |
|--------|----------|-------|
| Fields | **To** (username with autocomplete), **Thread title**, **Message** | `?to={username}` prefill from author profile |
| Actions | **Send** / **Cancel** | Rate-limited |

### Screen: Block user (modal from thread)

| Region | Elements | Notes |
|--------|----------|-------|
| Actions | **Block** / **Cancel** | Confirm modal |

### Screen: Blocked users (`GET /account/messages/blocked`)

| Region | Elements | Notes |
|--------|----------|-------|
| List | Blocked username, blocked at, **Unblock** | Manage pagination |

### Screen: Author profile — Message (`GET /authors/{username}`)

| Region | Elements | Notes |
|--------|----------|-------|
| Action | **Message** button | Signed-in only; guest → sign-in modal; links to compose with `?to=` |

### Screen: Administration — Message reports (`GET /administration/message-reports`)

| Region | Elements | Notes |
|--------|----------|-------|
| Audience | `ADMIN` only | Not `USER_ADMINISTRATOR` |
| List | Thread title, reporter, reported at, status | Manage pagination |
| Detail | Read-only thread snapshot, **Dismiss** / **Reviewed** | No edit of message bodies |

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | New `messaging`; extends `notification` (new types + optional FK); `navigation` (Account + Administration sections); `directory` (author profile CTA) |
| Packages | `dev.vepo.contraponto.messaging.*`, observers in `notification` |
| Routes | `/account/messages`, `/account/messages/{id}`, `/account/messages/compose`, `/account/messages/blocked`, `/forms/messages/*`, `/administration/message-reports`, author profile compose entry |
| Schema | `tb_message_threads`, `tb_thread_messages`, `tb_message_thread_participants`, `tb_user_blocks`, `tb_message_reports`; extend `tb_notifications` (nullable `blog_id`, `message_thread_id`) |
| Seed | `dev-import.sql` — sample open thread between `alice` and `dave`; optional flagged sample for admin |
| Tests | `MessageThread*Test`, `MessageThread*WebTest`, `UserBlock*Test`, `MessageReport*Test`, `BoundedContextRulesTest` |
| Docs | domain-spec, feature-catalog, cdi-events, ARCHITECTURE §9 |

### Risks

| Risk | Mitigation |
|------|------------|
| Spam / harassment | Rate limit new threads per user per day; block + flag queue |
| Naming collision with ActivityPub **inbox** | UI/domain term **mailbox** / **messages** only |
| `Notification.blog_id` NOT NULL today | Nullable when `message_thread_id` set (AQ1) |
| Cross-context coupling | `messaging` fires CDI events; `notification` observes — no reverse repo imports |

### Feature questions (FQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| FQ1 | Must users follow a blog before messaging? | answered | **No** — any logged-in user may message any other active user (with rate limits). |
| FQ2 | Can a closed thread be reopened? | answered | **No** — start a new thread with a new title. |
| FQ3 | Email when a new message arrives? | answered | **No** — in-app **notification** only. |
| FQ4 | Who reviews flags? | answered | **`ADMIN`** role only. |
| FQ5 | Max message body length? | answered | **2000** characters (same cap as **comment body**). |
| FQ6 | Compose entry points? | answered | Account hub **Compose** + **Message** on `/authors/{username}` (signed in). |
| FQ7 | Block behaviour on existing threads? | answered | **Freeze** thread; both participants see **User is blocked**; no new threads between the pair while block stands. |

## Architecture

See [architecture-design.mdc](../.cursor/rules/architecture-design.mdc).

### ADRs aplicáveis

| ADR | Relevância |
|-----|------------|
| [0002](../docs/adr/0002-backend-java-quarkus-jakarta-ee.md) | Quarkus + JPA |
| [0003](../docs/adr/0003-frontend-qute-htmx.md) | Account hub shell, HTMX forms, confirm modals |
| [0005](../docs/adr/0005-postgresql-database.md) | Flyway migrations |
| [0009](../docs/adr/0009-user-messaging-retention.md) | **Accepted** — flagged-thread retention and admin access |
| [0010](../docs/adr/0010-notification-retention.md) | **Accepted** — in-app notification purge (read 7d / unread 30d) |

### Design específico

| Area | Design |
|------|--------|
| **Bounded context** | `messaging` — reader engagement; may depend on `shared`, `user`, `auth` |
| **Aggregates** | `MessageThread` (title, initiator, recipient, status), `ThreadMessage` (body, author), `UserBlock` (blocker, blocked), `MessageReport` (thread, reporter, status) |
| **Thread status** | `OPEN` — replies allowed; `CLOSED` — either participant closed, no replies; `FROZEN` — active **user block** between participants, no replies, **User is blocked** banner for both |
| **Participant state** | `MessageThreadParticipant` per user: `lastReadAt` / `lastReadMessageId` for unread in mailbox |
| **Layers** | `MessageMailboxEndpoint`, `MessageThreadEndpoint`, `MessageComposeEndpoint`, `UserBlockEndpoint`, `MessageReportAdminEndpoint` → `MessageThreadService`, `MessageComposeService`, `UserBlockService`, `MessageReportService` → `*Repository` |
| **Access** | `MessageThreadAccess` — participant or `ADMIN` (report review); `UserBlockAccess` — blocker only for unblock |
| **URLs** | `MessageThreadPaths.mailbox()`, `.thread(id)`, `.compose(to)`, `.blocked()` — templates use `TemplateExtensions.url` |
| **Forms** | `POST /forms/messages/compose`, `POST /forms/messages/threads/{id}/reply`, `POST /forms/messages/threads/{id}/close`, `POST /forms/messages/threads/{id}/flag`, `POST /forms/messages/blocks/{blockedUserId}`, `POST /forms/messages/blocks/{blockedUserId}/unblock`, admin `POST /forms/administration/message-reports/{id}/dismiss` |
| **Navigation** | `NavigationHubRegistry` — Account Activity group: add `messages`, `messages/blocked` nested or sibling section `blocked`; Administration Platform group: `message-reports` (`ADMIN` gate) |
| **Notifications** | New types `NEW_MESSAGE_THREAD`, `NEW_THREAD_MESSAGE`; observer listens to `MessageThreadCreatedEvent`, `ThreadMessagePostedEvent`; link to `MessageThreadPaths.thread(id)`; `blog_id` nullable when `message_thread_id` set; subject to platform **notification retention** ([ADR-0010](../docs/adr/0010-notification-retention.md)) |
| **CDI events** | `MessageThreadCreatedEvent`, `ThreadMessagePostedEvent`, `MessageThreadClosedEvent`, `MessageThreadFlaggedEvent`, `UserBlockedEvent` — see [cdi-events.md](../docs/cdi-events.md) |
| **Rate limits** | `MessageComposeService` — e.g. max 10 new threads per user per rolling 24h (configurable `%dev` override) |
| **i18n** | Keys `messaging.*`, `account.nav.messages`, `administration.nav.messageReports`; PT-BR default |
| **CSS** | `manage.css` bundle (Account hub panels) |
| **Tests** | Unit: status transitions, block freezes thread, flag once per reporter; `@WebTest`: compose → reply → close; block shows banner both sides; admin report list (`admin` persona) |

### Architecture questions (AQ*n*)

| # | Question | Status | Answer |
|---|----------|--------|--------|
| AQ1 | How to attach message events to `Notification` (requires `blog_id` today)? | answered | Add nullable `message_thread_id` FK; make `blog_id` nullable when thread reference present; `actor` = other participant. |
| AQ2 | Separate header badge for mailbox unread? | answered | **MVP: no** — unread surfaced via existing notification bell + Account **Messages** section; optional mailbox badge in v2. |
| AQ3 | Retention policy for flagged threads? | answered | [ADR-0009](../docs/adr/0009-user-messaging-retention.md) — report rows 90 days; participant threads unchanged on dismiss |

### Screen: Account hub — Messages (`GET /account/messages`)

| Region | Elements | Notes |
|--------|----------|-------|
| Hub nav | **Messages** under Activity group | Alongside Notifications, Subscriptions |
| Tabs | **Open** / **Closed** | Manage pagination per tab |
| List row | Thread title, other participant, preview, unread indicator | Library-style rows |
| Primary action | **Compose** | Opens compose panel |

```
┌─ Account ─ Messages ─────────────────────────────┐
│ [ Open ] [ Closed ]                    [ Compose ] │
├──────────────────────────────────────────────────┤
│ ● Re: collaboration     alice → you    2h ago      │
│   Would you be interested in…                    │
├──────────────────────────────────────────────────┤
│   Question about series  you → bob     yesterday   │
└──────────────────────────────────────────────────┘
```

### Screen: Thread (`GET /account/messages/{threadId}`)

| Region | Elements | Notes |
|--------|----------|-------|
| Header | Thread title, participant name | Breadcrumb via Account hub |
| Banner (frozen) | **User is blocked** | Both participants when block active |
| Messages | Chronological **thread messages** | Author + timestamp |
| Actions | **Reply**, **Close thread**, **Flag thread**, **Block user** | Confirm modal for destructive actions |
| Reply form | Textarea + **Send reply** | Hidden when closed or frozen |

### Screen: Compose (`GET /account/messages/compose`)

| Region | Elements | Notes |
|--------|----------|-------|
| Fields | **To** (username with autocomplete), **Thread title**, **Message** | `?to={username}` prefill from author profile |
| Actions | **Send** / **Cancel** | Rate-limited |

### Screen: Block user (modal from thread)

| Region | Elements | Notes |
|--------|----------|-------|
| Actions | **Block** / **Cancel** | Confirm modal |

### Screen: Blocked users (`GET /account/messages/blocked`)

| Region | Elements | Notes |
|--------|----------|-------|
| List | Blocked username, blocked at, **Unblock** | Manage pagination |

### Screen: Author profile — Message (`GET /authors/{username}`)

| Region | Elements | Notes |
|--------|----------|-------|
| Action | **Message** button | Signed-in only; guest → sign-in modal; links to compose with `?to=` |

### Screen: Administration — Message reports (`GET /administration/message-reports`)

| Region | Elements | Notes |
|--------|----------|-------|
| Audience | `ADMIN` only | Not `USER_ADMINISTRATOR` |
| List | Thread title, reporter, reported at, status | Manage pagination |
| Detail | Read-only thread snapshot, **Dismiss** / **Reviewed** | No edit of message bodies |

## Impact

| Area | Effect |
|------|--------|
| Bounded contexts | New `messaging`; extends `notification` (new types + optional FK); `navigation` (Account + Administration sections); `directory` (author profile CTA) |
| Packages | `dev.vepo.contraponto.messaging.*`, observers in `notification` |
| Routes | `/account/messages`, `/account/messages/{id}`, `/account/messages/compose`, `/account/messages/blocked`, `/forms/messages/*`, `/administration/message-reports`, author profile compose entry |
| Schema | `tb_message_threads`, `tb_thread_messages`, `tb_message_thread_participants`, `tb_user_blocks`, `tb_message_reports`; extend `tb_notifications` (nullable `blog_id`, `message_thread_id`) |
| Seed | `dev-import.sql` — sample open thread between `alice` and `dave`; optional flagged sample for admin |
| Tests | `MessageThread*Test`, `MessageThread*WebTest`, `UserBlock*Test`, `MessageReport*Test`, `BoundedContextRulesTest` |
| Docs | domain-spec, feature-catalog, cdi-events, ARCHITECTURE §9 |

#### Feature checklist

| ID | Criterion | Source | Done |
|----|-----------|--------|------|
| FC1 | Mailbox Open/Closed tabs with pagination | FQ2 | ☑ |
| FC2 | Compose thread with title + first message (2000 chars) | FQ5 | ☑ |
| FC3 | Reply on open threads | — | ☑ |
| FC4 | Close thread — no further replies | FQ2 | ☑ |
| FC5 | Flag thread — admin queue (`ADMIN`) | FQ4 | ☑ |
| FC6 | Block user; freeze thread; **User is blocked** both sides | FQ7 | ☑ |
| FC7 | Blocked users list + unblock | FQ7 | ☑ |
| FC8 | In-app notification on new thread / new reply (no email) | FQ3 | ☑ |
| FC9 | **Message** on author profile → compose | FQ6 | ☑ |
| FC10 | Any user may message any active user (rate limited) | FQ1 | ☑ |
| FC11 | Message report purge after 90 days (report row only) | ADR-0009 | ☑ |
| FC12 | domain-spec + cdi-events + feature-catalog updated | Docs | ☑ |
| FCdev | `alice` ↔ `dave` sample thread in `dev-import.sql` | dev-import | ☑ |

#### Tasks (phase 3)

| ID | Task | Done |
|----|------|------|
| T1 | Flyway `V0.0.11__user_messaging.sql`: threads, messages, participants, blocks, reports; `tb_notifications` nullable `blog_id` + `message_thread_id` | ☑ |
| T2 | Entities + repositories: `MessageThread`, `ThreadMessage`, `MessageThreadParticipant`, `UserBlock`, `MessageReport` | ☑ |
| T3 | `MessageThreadAccess`, `UserBlockAccess` | ☑ |
| T4 | `MessageThreadPaths` + `TemplateExtensions` wiring | ☑ |
| T5 | `MessageComposeService` — create thread + first message; rate limit (10/24h) | ☑ |
| T6 | `MessageThreadService` — reply, close, flag, mark read / participant unread | ☑ |
| T7 | `UserBlockService` — block, unblock; `MessageThreadFreezeObserver` on `UserBlockedEvent` | ☑ |
| T8 | CDI events (`MessageThreadCreatedEvent`, `ThreadMessagePostedEvent`, …) + `MessageNotificationObserver` | ☑ |
| T9 | `NotificationType.NEW_MESSAGE_THREAD`, `NEW_THREAD_MESSAGE` + nullable blog + thread FK on `Notification` | ☑ |
| T10 | Account hub: `messages` + `blocked` sections in `NavigationHubRegistry`; mailbox Open/Closed templates | ☑ |
| T11 | Thread view template + reply form; frozen/closed banners | ☑ |
| T12 | Compose template + `MessageComposeEndpoint` (`?to=` prefill) | ☑ |
| T13 | Form endpoints: reply, close, flag, block, unblock (confirm modals) | ☑ |
| T14 | Administration `message-reports` hub section + list/detail templates (`ADMIN` gate) | ☑ |
| T15 | Author profile **Message** button → compose (`AuthorProfileEndpoint`) | ☑ |
| T16 | `MessageReportRetentionScheduler` — purge report rows older than 90 days ([ADR-0009](../docs/adr/0009-user-messaging-retention.md)) | ☑ |
| T17 | i18n keys (`messaging.*`, hub nav labels) PT-BR / EN / ES | ☑ |
| T18 | ArchUnit: `messaging` bounded-context dependency rules | ☑ |
| Tdev | `dev-import.sql` sample thread + [feature-catalog.md](../docs/feature-catalog.md) § Dev personas / routes | ☑ |

#### Test coverage (phase 3)

| ID | Test | Covers | Done |
|----|------|--------|------|
| TC1 | `MessageComposeServiceTest` — create thread, rate limit, block prevents compose | T5, T7, FC10 | ☑ |
| TC2 | `MessageThreadServiceTest` — reply, close, flag idempotency | T6, FC3–FC5 | ☑ |
| TC3 | `UserBlockServiceTest` — freeze open threads; banner state both sides | T7, FC6 | ☑ |
| TC4 | `MessageNotificationObserverTest` — NEW_MESSAGE_* created, links to thread | T8, T9, FC8 | ☑ |
| TC5 | `MessageThreadWebTest` — compose → reply → close flow | T10–T13, FC1–FC4 | ☑ |
| TC6 | `MessageBlockWebTest` — block freezes thread; blocked list + unblock | T7, T10, T13, FC6–FC7 | ☑ |
| TC7 | `MessageReportAdminTest` — admin sees flagged thread; dismiss report | T14, FC5 | ☑ |
| TC8 | `AuthorProfileMessageWebTest` — Message button → compose prefill | T15, FC9 | ☑ |
| TC9 | `MessageReportRetentionServiceTest` — 90-day report purge | T16, FC11 | ☑ |
| TC10 | `BoundedContextRulesTest` includes `messaging` | T18 | ☑ |

**Development approval:** approved 2026-07-07 — tasks: T1–T18, Tdev

**Implementation notes:** Package `dev.vepo.contraponto.messaging`; compose uses `UserRepository.findActiveByUsername` (any active user, FQ1); notification queries use `LEFT JOIN FETCH` for nullable `blog_id`; migrations `V0.0.10` (notification `read_at`) + `V0.0.11` (messaging tables). `verify` green.

### Remove block reason — 2026-07-07

**Version:** 1.1  
**Status:** done

**Description:** Blocking no longer collects or stores a reason. `tb_user_blocks.reason` removed from `V0.0.11` (not yet deployed); block confirm modal is title/message/actions only.

**Impact on other features:** None — UI copy and domain vocabulary only.

**Development approval:** approved 2026-07-07 — scope: feature doc, Flyway `V0.0.11`, domain spec, messaging block flow.
