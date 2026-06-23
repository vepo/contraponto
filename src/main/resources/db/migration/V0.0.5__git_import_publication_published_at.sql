-- Git import sets tb_posts.published_at from front matter but first publication snapshots
-- were stamped at sync time. Align live publication timestamps with the post date.
UPDATE tb_post_publications pub
SET published_at = p.published_at
FROM tb_posts p
         JOIN tb_blogs b ON b.id = p.blog_id
WHERE pub.id = p.live_publication_id
  AND b.git_enabled = TRUE
  AND p.published_at IS NOT NULL
  AND pub.published_at IS DISTINCT FROM p.published_at;
