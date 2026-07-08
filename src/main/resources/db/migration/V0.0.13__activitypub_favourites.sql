CREATE TABLE tb_activitypub_favourites (
    id                  BIGSERIAL PRIMARY KEY,
    post_id             BIGINT NOT NULL REFERENCES tb_posts(id) ON DELETE CASCADE,
    remote_actor_id     BIGINT NOT NULL REFERENCES tb_activitypub_remote_actors(id) ON DELETE CASCADE,
    like_activity_id    VARCHAR(2048) NOT NULL UNIQUE,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (post_id, remote_actor_id)
);

CREATE INDEX idx_activitypub_favourites_post
    ON tb_activitypub_favourites (post_id);
