# Fediverse / ActivityPub database operations

Console SQL for inspecting and repairing ActivityPub federation state in `%dev` or production.

**Related**

| Doc | Use when |
|-----|----------|
| [feature/activitypub-integration.md](../feature/activitypub-integration.md) | Product behaviour and routes |
| [domain-specification.md](domain-specification.md) § ActivityPub federation | Ubiquitous language |
| [mastodon-remote-account-resolution.md](mastodon-remote-account-resolution.md) | How remotes discover local actors |
| [rest-url-guide.md](rest-url-guide.md) §11 | Public vs `/__activity_pub__/` ingress |
| Migrations | `V0.0.8`, `V0.0.9`, `V0.0.12` |

---

## Table of contents

1. [Conventions](#conventions)
2. [Schema map](#schema-map)
3. [Quick health check](#1-quick-health-check)
4. [Follows](#2-follows)
5. [Outbound deliveries](#3-outbound-deliveries)
6. [Common repairs](#4-common-repairs)
7. [Troubleshooting](#5-troubleshooting)
8. [Safety notes](#safety-notes)

---

## Conventions

| Rule | Detail |
|------|--------|
| Lookup by username | Always join `tb_users` on `username`. Never hard-code numeric IDs. |
| Placeholder | Replace `'vepo'` with the local author you care about. |
| Reads first | Run the diagnostic `SELECT`s before any `UPDATE` / `DELETE`. |
| Transactions | Wrap multi-statement repairs in `BEGIN` / `COMMIT` (or `ROLLBACK` if unsure). |
| Secrets | Never paste production passwords or `private_key_encrypted` / PEM material into this file or chat logs. |

Typical workflow:

```text
1. Health overview (platform + actor + counts)
2. List follows / stuck PENDING
3. Inspect pending & failed deliveries (+ last_error)
4. Apply the narrowest repair
5. Re-run health counts to confirm
```

---

## Schema map

| Table | Purpose |
|-------|---------|
| `tb_activitypub_platform_settings` | Global kill-switch (`federation_enabled`, row `id = 1`) |
| `tb_activitypub_actors` | One local Person actor per `User` (keys + per-user opt-in) |
| `tb_activitypub_remote_actors` | Cached remote Person (inbox, signing key, display fields) |
| `tb_activitypub_follows` | Remote → local follow (`PENDING` / `ACCEPTED` / `REJECTED`) |
| `tb_activitypub_deliveries` | Outbound signed POSTs to remote inboxes |

```text
tb_users
   └── tb_activitypub_actors          (1:1 on user_id)
         ├── tb_activitypub_follows ──► tb_activitypub_remote_actors
         └── tb_activitypub_deliveries ──► target_inbox_url (remote)
```

### Status and activity values

**Follow — `tb_activitypub_follows.status`**

| Value | Meaning |
|-------|---------|
| `PENDING` | Legacy / stuck request — new verified inbox follows are auto-accepted |
| `ACCEPTED` | Remote is a follower; eligible for Create fan-out and backfill |
| `REJECTED` | Author or flow rejected the follow |

**Delivery — `tb_activitypub_deliveries.status`**

| Value | Meaning |
|-------|---------|
| `PENDING` | Queued or waiting for retry (`next_retry_at`) |
| `DELIVERED` | Remote inbox accepted the POST |
| `FAILED` | Exhausted retries (`last_error` usually set) |

**Activity type — `tb_activitypub_deliveries.activity_type`**

`CREATE` · `UPDATE` · `DELETE` · `ACCEPT` · `REJECT`

**Create backfill volume (v1.4+):** Accept / re-Follow historical **Create** backfill and the author outbox include published posts from **all** of the author’s **active** blogs (main and secondary), ordered by `published_at`. After deploy, expect higher delivery counts than the former main-blog-only MVP when authors have secondary-blog archives; re-queue FAILED Creates (section 4) if remotes still lack history.

---

## 1. Quick health check

### 1.1 Platform + local actor

```sql
SELECT ps.federation_enabled AS platform_enabled,
       ps.updated_at         AS platform_updated_at
FROM tb_activitypub_platform_settings ps
WHERE ps.id = 1;

SELECT u.username,
       a.id AS local_actor_id,
       a.federation_enabled,
       a.public_key_id,
       a.created_at,
       a.updated_at
FROM tb_activitypub_actors a
JOIN tb_users u ON u.id = a.user_id
WHERE u.username = 'vepo';
```

If `platform_enabled` or the user’s `federation_enabled` is `FALSE`, remotes get 404 on actor/outbox and outbound deliveries are blocked.

### 1.2 Counts for one author

```sql
SELECT
  (SELECT COUNT(*)
     FROM tb_activitypub_follows f
     JOIN tb_activitypub_actors a ON a.id = f.local_actor_id
     JOIN tb_users u ON u.id = a.user_id
    WHERE u.username = 'vepo' AND f.status = 'ACCEPTED') AS accepted_followers,
  (SELECT COUNT(*)
     FROM tb_activitypub_follows f
     JOIN tb_activitypub_actors a ON a.id = f.local_actor_id
     JOIN tb_users u ON u.id = a.user_id
    WHERE u.username = 'vepo' AND f.status = 'PENDING') AS pending_follows,
  (SELECT COUNT(*)
     FROM tb_activitypub_deliveries d
     JOIN tb_activitypub_actors a ON a.id = d.local_actor_id
     JOIN tb_users u ON u.id = a.user_id
    WHERE u.username = 'vepo' AND d.status = 'PENDING') AS pending_deliveries,
  (SELECT COUNT(*)
     FROM tb_activitypub_deliveries d
     JOIN tb_activitypub_actors a ON a.id = d.local_actor_id
     JOIN tb_users u ON u.id = a.user_id
    WHERE u.username = 'vepo' AND d.status = 'FAILED') AS failed_deliveries;
```

Interpret:

| Signal | Next step |
|--------|-----------|
| No actor row | User never opted in (or actor was deleted) |
| `pending_follows` > 0 | [§2.3 Stuck PENDING](#23-follows-stuck-in-pending) |
| `pending_deliveries` / `failed_deliveries` > 0 | [§3 Deliveries](#3-outbound-deliveries) then [§4 Repairs](#4-common-repairs) |

---

## 2. Follows

### 2.1 List follows for a local author

```sql
SELECT f.id,
       f.status,
       f.created_at,
       f.accepted_at,
       f.follow_activity_id,
       r.actor_id,
       r.preferred_username,
       r.display_name,
       r.inbox_url,
       r.public_key_id,
       r.profile_fetched_at
FROM tb_activitypub_follows f
JOIN tb_activitypub_actors a ON a.id = f.local_actor_id
JOIN tb_activitypub_remote_actors r ON r.id = f.remote_actor_id
JOIN tb_users u ON u.id = a.user_id
WHERE u.username = 'vepo'
ORDER BY f.created_at DESC;
```

### 2.2 Find a remote by actor URL or host

```sql
SELECT r.id,
       r.actor_id,
       r.preferred_username,
       r.inbox_url,
       r.public_key_id,
       r.profile_fetched_at,
       r.public_key_pem IS NOT NULL AS has_public_key
FROM tb_activitypub_remote_actors r
WHERE r.actor_id ILIKE '%mstdn.social%'
   OR r.inbox_url ILIKE '%mastodon.acm.org%'
ORDER BY r.id DESC;
```

### 2.3 Follows stuck in PENDING

```sql
SELECT f.id, f.created_at, r.actor_id, r.inbox_url
FROM tb_activitypub_follows f
JOIN tb_activitypub_actors a ON a.id = f.local_actor_id
JOIN tb_activitypub_remote_actors r ON r.id = f.remote_actor_id
JOIN tb_users u ON u.id = a.user_id
WHERE u.username = 'vepo'
  AND f.status = 'PENDING'
ORDER BY f.created_at;
```

New verified follows are auto-accepted. Old `PENDING` rows usually need a re-follow from the remote, or a manual accept in the author UI if that path is still enabled.

---

## 3. Outbound deliveries

### 3.1 Pending and failed rows

```sql
SELECT d.id,
       d.activity_type,
       d.status,
       d.attempts,
       d.next_retry_at,
       d.last_error,
       d.target_inbox_url,
       d.object_id,
       d.created_at,
       d.delivered_at
FROM tb_activitypub_deliveries d
JOIN tb_activitypub_actors a ON a.id = d.local_actor_id
JOIN tb_users u ON u.id = a.user_id
WHERE u.username = 'vepo'
  AND d.status IN ('PENDING', 'FAILED')
ORDER BY d.created_at DESC
LIMIT 50;
```

### 3.2 Summary by status and activity type

```sql
SELECT d.status, d.activity_type, COUNT(*) AS n
FROM tb_activitypub_deliveries d
JOIN tb_activitypub_actors a ON a.id = d.local_actor_id
JOIN tb_users u ON u.id = a.user_id
WHERE u.username = 'vepo'
GROUP BY d.status, d.activity_type
ORDER BY d.status, d.activity_type;
```

### 3.3 Summary by inbox (fleet-wide)

Useful when one remote host is drowning the queue:

```sql
SELECT d.activity_type,
       d.status,
       d.target_inbox_url,
       COUNT(*) AS n
FROM tb_activitypub_deliveries d
GROUP BY d.target_inbox_url, d.status, d.activity_type
ORDER BY n DESC, d.target_inbox_url;
```

### 3.4 Peek at a payload (title / object URL)

```sql
SELECT d.id,
       d.activity_type,
       d.status,
       d.object_id,
       left(d.payload_json, 400) AS payload_preview
FROM tb_activitypub_deliveries d
JOIN tb_activitypub_actors a ON a.id = d.local_actor_id
JOIN tb_users u ON u.id = a.user_id
WHERE u.username = 'vepo'
ORDER BY d.id DESC
LIMIT 20;
```

Post object URLs should look like platform paths with a username prefix, e.g.  
`https://blogs.commit-mestre.dev/vepo/post/{slug}` — not bare `/posts/...` on an IP.

---

## 4. Common repairs

Prefer the smallest change that restores service. Re-run §1.2 after each repair.

### 4.1 Retry stuck PENDING deliveries

Resets attempts so the scheduler picks them up again (status stays `PENDING`):

```sql
UPDATE tb_activitypub_deliveries d
SET next_retry_at = NULL,
    attempts = 0,
    last_error = NULL
FROM tb_activitypub_actors a
JOIN tb_users u ON u.id = a.user_id
WHERE d.local_actor_id = a.id
  AND u.username = 'vepo'
  AND d.status = 'PENDING';
```

### 4.2 Re-queue FAILED → PENDING

```sql
UPDATE tb_activitypub_deliveries d
SET status = 'PENDING',
    next_retry_at = NULL,
    attempts = 0,
    last_error = NULL
FROM tb_activitypub_actors a
JOIN tb_users u ON u.id = a.user_id
WHERE d.local_actor_id = a.id
  AND u.username = 'vepo'
  AND d.status = 'FAILED';
```

### 4.3 Retry PENDING and FAILED together

When you want one reset for both statuses (same author):

```sql
UPDATE tb_activitypub_deliveries d
SET status = 'PENDING',
    next_retry_at = NULL,
    attempts = 0,
    last_error = NULL
FROM tb_activitypub_actors a
JOIN tb_users u ON u.id = a.user_id
WHERE d.local_actor_id = a.id
  AND u.username = 'vepo'
  AND d.status IN ('PENDING', 'FAILED');
```

### 4.4 Reset federation for one author

**Destructive.** Deletes that author’s deliveries and follows, then drops orphan remote actors nobody follows anymore. Remotes must Follow again afterward (auto-accept + historical Create backfill run on accept).

```sql
BEGIN;

DELETE FROM tb_activitypub_deliveries
WHERE local_actor_id IN (
  SELECT a.id FROM tb_activitypub_actors a
  JOIN tb_users u ON u.id = a.user_id
  WHERE u.username = 'vepo'
);

DELETE FROM tb_activitypub_follows
WHERE local_actor_id IN (
  SELECT a.id FROM tb_activitypub_actors a
  JOIN tb_users u ON u.id = a.user_id
  WHERE u.username = 'vepo'
);

DELETE FROM tb_activitypub_remote_actors r
WHERE NOT EXISTS (
  SELECT 1 FROM tb_activitypub_follows f WHERE f.remote_actor_id = r.id
);

COMMIT;
```

### 4.5 Clear a dead remote (e.g. HTTP 410 on actor fetch)

If a remote actor is gone (`410 Gone`) and keeps failing verification or deliveries:

```sql
-- Inspect
SELECT r.id, r.actor_id, r.inbox_url, r.profile_fetched_at
FROM tb_activitypub_remote_actors r
WHERE r.actor_id = 'https://mstdn.social/ap/users/116619700957379404';

-- Remove follow edges, then the remote (only if you intend to drop that follower)
BEGIN;

DELETE FROM tb_activitypub_follows
WHERE remote_actor_id = (
  SELECT id FROM tb_activitypub_remote_actors
  WHERE actor_id = 'https://mstdn.social/ap/users/116619700957379404'
);

DELETE FROM tb_activitypub_remote_actors
WHERE actor_id = 'https://mstdn.social/ap/users/116619700957379404';

COMMIT;
```

Inbound POSTs from deleted remotes will still get **401** (no fetchable public key) — that is expected, not a local routing bug.

---

## 5. Troubleshooting

| Log / symptom | Likely cause | DB check / action |
|---------------|--------------|-------------------|
| `crypto mismatch` + `requestTarget=post /vepo/inbox` on subdomain Host | Signature path rewrite bug (should keep `/inbox`) | App / ingress fix — not a DB row |
| `Remote actor fetch HTTP 410` + `no public key` | Remote deleted or moved actor | Remote `actor_id` row; optional [§4.5](#45-clear-a-dead-remote-eg-http-410-on-actor-fetch) |
| Follows stay `PENDING` forever | Pre-auto-accept leftovers | [§2.3](#23-follows-stuck-in-pending); ask remote to re-follow |
| Unfollow then re-follow does nothing | Old bug: `REJECTED` row blocked new Follow | Fixed: reopen + auto-accept; check `status` + `accepted_at` |
| Many `FAILED` deliveries | Network, remote inbox, bad URL, or signature reject (`HTTP 401`) | `last_error`, `attempts`, `target_inbox_url` — then [§4.2](#42-re-queue-failed--pending) |
| Outbound CREATE → Mastodon `HTTP 401` while actor GET/HEAD is 200 | (1) Actor HEAD was **406** (fixed). (2) **Date** header used Java RFC-1123 without zero-padded day (`Wed, 8 Jul…`); Ruby `Time.httpdate` rejects → 401. Fix: `EEE, dd MMM yyyy HH:mm:ss GMT`. (3) Confirm image has `-Djdk.httpclient.allowRestrictedHeaders=host` so signed `Host` is on the wire | Deploy fix; [§4.2](#42-re-queue-failed--pending) |
| `FAILED` with empty `last_error` | Older bug: null exception message; or need newer logging | Re-queue after deploy; watch app logs for `ActivityPub delivery` |
| `federation_enabled = false` | User opted out or platform kill-switch | [§1.1](#11-platform--local-actor) |
| Object IDs use IP / bare `/posts/...` | Wrong public URL configuration | Payload peek [§3.4](#34-peek-at-a-payload-title--object-url); fix app config, then re-queue Creates |

---

## Safety notes

- Do **not** put production passwords or private key PEM contents into this file or chat logs.
- `dev-import.sql` is for `%dev` seed only — production diagnostics use the queries above on the live DB.
- Public federation URLs stay as Mastodon/Friendica expect; internal JAX-RS routes use `/__activity_pub__/...` after ingress rewrite ([rest-url-guide.md](rest-url-guide.md) §11).
- Prefer scoped deletes (one username, one `actor_id`) over fleet-wide `TRUNCATE`.
