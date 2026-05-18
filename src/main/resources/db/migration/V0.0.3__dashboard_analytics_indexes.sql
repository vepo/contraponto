CREATE INDEX idx_views_post_viewed_at ON tb_views (post_id, viewed_at);

CREATE INDEX idx_notifications_blog_type_created ON tb_notifications (blog_id, type, created_at);
