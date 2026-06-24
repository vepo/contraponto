-- Remove view and reading-time rows where the viewer is the post author (blog owner).
-- One-time cleanup for data recorded before author self-access exclusion.

DELETE FROM tb_views v
USING tb_posts p, tb_blogs b
WHERE v.post_id = p.id
  AND p.blog_id = b.id
  AND v.user_id IS NOT NULL
  AND v.user_id = b.owner_id;

DELETE FROM tb_reading_sessions rs
USING tb_posts p, tb_blogs b
WHERE rs.post_id = p.id
  AND p.blog_id = b.id
  AND rs.user_id IS NOT NULL
  AND rs.user_id = b.owner_id;
