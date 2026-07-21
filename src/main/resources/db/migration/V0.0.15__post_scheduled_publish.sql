ALTER TABLE tb_posts
    ADD COLUMN scheduled_at TIMESTAMP NULL;

CREATE INDEX idx_posts_scheduled_due ON tb_posts (scheduled_at)
    WHERE published = false AND scheduled_at IS NOT NULL;
