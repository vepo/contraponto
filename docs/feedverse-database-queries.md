# Feedverse / ActivityPub database guide

Console SQL for inspecting and repairing local ActivityPub federation state in production or `%dev`.

Replace `'vepo'` with the local username you care about. Prefer looking up users by `username`, never by hard-coded numeric IDs.

**Related:** [feature/activitypub-integration.md](../feature/activitypub-integration.md), [domain-specification.md](domain-specification.md) § ActivityPub federation, migrations `V0.0.8` / `V0.0.9` / `V0.0.12`.

---

## Schema map

| Table | Purpose |
|-------|---------|
| `tb_activitypub_platform_settings` | Global kill-switch (`federation_enabled`) |
| `tb_activitypub_actors` | One local Person actor per `User` (keys + opt-in) |
| `tb_activitypub_remote_actors` | Cached remote Person (inbox + signing key) |
| `tb_activitypub_follows` | Remote → local follow (`PENDING` / `ACCEPTED` / `REJECTED`) |
| `tb_activitypub_deliveries` | Outbound signed POSTs to remote inboxes |

```text
tb_users
   └── tb_activitypub_actors (1:1)
         ├── tb_activitypub_follows ──► tb_activitypub_remote_actors
         └── tb_activitypub_deliveries ──► target_inbox_url (remote)
```

### Status values

**Follow (`tb_activitypub_follows.status`)**

| Value | Meaning |
|-------|---------|
| `PENDING` | Legacy / stuck request — new inbox follows are auto-accepted |
| `ACCEPTED` | Remote is a follower; eligible for Create fan-out and backfill |
| `REJECTED` | Author or flow rejected the follow |

**Delivery (`tb_activitypub_deliveries.status`)**

| Value | Meaning |
|-------|---------|
| `PENDING` | Queued or waiting for retry (`next_retry_at`) |
| `DELIVERED` | Remote inbox accepted the POST |
| `FAILED` | Exhausted retries (`last_error` set) |

**Activity type (`tb_activitypub_deliveries.activity_type`)**  
`CREATE` · `UPDATE` · `DELETE` · `ACCEPT` · `REJECT`

---

## Health overview

### Platform + local actor

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

If `platform_enabled` or the user’s `federation_enabled` is `FALSE`, remotes get 404 on actor/outbox and deliveries are blocked.

### Counts for one author

```sql
SELECT
  (SELECT COUNT(*) FROM tb_activitypub_follows f
     JOIN tb_activitypub_actors a ON a.id = f.local_actor_id
     JOIN tb_users u ON u.id = a.user_id
    WHERE u.username = 'vepo' AND f.status = 'ACCEPTED') AS accepted_followers,
  (SELECT COUNT(*) FROM tb_activitypub_follows f
     JOIN tb_activitypub_actors a ON a.id = f.local_actor_id
     JOIN tb_users u ON u.id = a.user_id
    WHERE u.username = 'vepo' AND f.status = 'PENDING') AS pending_follows,
  (SELECT COUNT(*) FROM tb_activitypub_deliveries d
     JOIN tb_activitypub_actors a ON a.id = d.local_actor_id
     JOIN tb_users u ON u.id = a.user_id
    WHERE u.username = 'vepo' AND d.status = 'PENDING') AS pending_deliveries,
  (SELECT COUNT(*) FROM tb_activitypub_deliveries d
     JOIN tb_activitypub_actors a ON a.id = d.local_actor_id
     JOIN tb_users u ON u.id = a.user_id
    WHERE u.username = 'vepo' AND d.status = 'FAILED') AS failed_deliveries;
```

---

## Follows (who follows whom)

### List follows for a local author

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

### Find a remote by actor URL or host

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

### Follows stuck in PENDING (pre-auto-accept leftovers)

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

## Deliveries (outbound queue)

### Pending / failed deliveries

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

### Peek at a payload (title / object URL)

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

### Delivery summary by status and activity

```sql
SELECT d.status, d.activity_type, COUNT(*) AS n
FROM tb_activitypub_deliveries d
JOIN tb_activitypub_actors a ON a.id = d.local_actor_id
JOIN tb_users u ON u.id = a.user_id
WHERE u.username = 'vepo'
GROUP BY d.status, d.activity_type
ORDER BY d.status, d.activity_type;
```

---

## Common repairs

### Retry stuck PENDING deliveries

Resets attempts so the scheduler picks them up again:

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

### Re-queue FAILED → PENDING (same reset)

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

### Reset federation for one author (re-follow needed)

Deletes local deliveries and follows for that actor, then drops orphan remote actors nobody follows anymore.

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

After this, remotes must Follow again (auto-accept + historical Create backfill run on accept).

### Clear a dead remote (e.g. HTTP 410 on actor fetch)

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

## Interpreting inbox / fetch logs

| Log / symptom | Likely cause | DB check |
|---------------|--------------|----------|
| `crypto mismatch` + `requestTarget=post /vepo/inbox` on subdomain Host | Signature path rewrite bug (should keep `/inbox`) | App version / ingress fix |
| `Remote actor fetch HTTP 410` + `no public key` | Remote deleted/moved actor | Remote `actor_id` row; optional cleanup above |
| Follows stay `PENDING` forever | Pre-auto-accept leftovers | `status = 'PENDING'` list |
| Many `FAILED` deliveries | Network / remote inbox / bad URL | `last_error`, `target_inbox_url`, `object_id` |
| `federation_enabled = false` | User opted out or kill-switch | Actor + platform settings |

---

## Notes

- Do **not** put production passwords or private key PEM contents into this file or chat logs.
- `dev-import.sql` is for `%dev` seed only — production diagnostics use the queries above on the live DB.
- Public federation URLs stay as Mastodon/Friendica expect; internal JAX-RS routes use `/__activity_pub__/...` after ingress rewrite ([rest-url-guide.md](rest-url-guide.md) §11).
