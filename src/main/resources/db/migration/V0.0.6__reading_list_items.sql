CREATE TABLE tb_reading_list_items (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES tb_users(id) ON DELETE CASCADE,
    post_id     BIGINT NOT NULL REFERENCES tb_posts(id) ON DELETE CASCADE,
    saved_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    read_at     TIMESTAMP,
    UNIQUE (user_id, post_id)
);

CREATE INDEX idx_reading_list_user_unread
    ON tb_reading_list_items (user_id, saved_at)
    WHERE read_at IS NULL;

CREATE INDEX idx_reading_list_user_all
    ON tb_reading_list_items (user_id, read_at DESC NULLS FIRST, saved_at DESC);
