CREATE TABLE tb_post_reactions (
    id              BIGSERIAL PRIMARY KEY,
    post_id         BIGINT NOT NULL REFERENCES tb_posts(id) ON DELETE CASCADE,
    user_id         BIGINT NOT NULL REFERENCES tb_users(id) ON DELETE CASCADE,
    reaction_type   VARCHAR(20) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (post_id, user_id, reaction_type)
);

CREATE INDEX idx_post_reactions_post
    ON tb_post_reactions (post_id);
