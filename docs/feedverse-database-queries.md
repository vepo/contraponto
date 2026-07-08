# Feedverse Database Queries

## Cleanup

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

## List follows

```sql
SELECT a.federation_enabled, f.id, f.status, r.actor_id
FROM tb_activitypub_follows f
JOIN tb_activitypub_actors a ON a.id = f.local_actor_id
JOIN tb_activitypub_remote_actors r ON r.id = f.remote_actor_id
JOIN tb_users u ON u.id = a.user_id
WHERE u.username = 'vepo';
```