CREATE TABLE tb_reading_sessions (
    id               BIGSERIAL PRIMARY KEY,
    post_id          BIGINT NOT NULL REFERENCES tb_posts(id) ON DELETE CASCADE,
    user_id          BIGINT REFERENCES tb_users(id) ON DELETE SET NULL,
    session_id       VARCHAR(255) NOT NULL,
    started_at       TIMESTAMP NOT NULL,
    last_activity_at TIMESTAMP NOT NULL,
    total_seconds    INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uq_reading_session_post_session UNIQUE (post_id, session_id)
);

CREATE INDEX idx_reading_sessions_post_activity ON tb_reading_sessions (post_id, last_activity_at);
