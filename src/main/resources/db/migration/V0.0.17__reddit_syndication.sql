ALTER TABLE tb_blogs
    ADD COLUMN reddit_syndication_enabled BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE tb_user_reddit_accounts (
    id                          BIGSERIAL PRIMARY KEY,
    user_id                     BIGINT NOT NULL UNIQUE REFERENCES tb_users(id) ON DELETE CASCADE,
    reddit_username             VARCHAR(255) NOT NULL,
    encrypted_refresh_token     TEXT NOT NULL,
    linked_at                   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE tb_reddit_post_records (
    id                      BIGSERIAL PRIMARY KEY,
    post_id                 BIGINT NOT NULL UNIQUE REFERENCES tb_posts(id) ON DELETE CASCADE,
    submission_fullname     VARCHAR(32) NOT NULL,
    permalink               VARCHAR(512),
    created_at              TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE tb_reddit_deliveries (
    id                  BIGSERIAL PRIMARY KEY,
    post_id             BIGINT NOT NULL REFERENCES tb_posts(id) ON DELETE CASCADE,
    user_id             BIGINT NOT NULL REFERENCES tb_users(id) ON DELETE CASCADE,
    kind                VARCHAR(16) NOT NULL,
    status              VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    attempts            INT NOT NULL DEFAULT 0,
    next_retry_at       TIMESTAMP,
    last_error          TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    delivered_at        TIMESTAMP
);

CREATE INDEX idx_reddit_deliveries_pending
    ON tb_reddit_deliveries (status, next_retry_at);

CREATE TABLE tb_reddit_platform_settings (
    id                  INT PRIMARY KEY,
    syndication_enabled BOOLEAN NOT NULL,
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO tb_reddit_platform_settings (id, syndication_enabled, updated_at)
VALUES (1, TRUE, NOW());
