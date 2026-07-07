CREATE TABLE tb_activitypub_actors (
    id                      BIGSERIAL PRIMARY KEY,
    user_id                 BIGINT NOT NULL UNIQUE REFERENCES tb_users(id) ON DELETE CASCADE,
    federation_enabled      BOOLEAN NOT NULL DEFAULT FALSE,
    private_key_encrypted   TEXT NOT NULL,
    public_key_pem          TEXT NOT NULL,
    public_key_id           VARCHAR(2048) NOT NULL,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE tb_activitypub_remote_actors (
    id                  BIGSERIAL PRIMARY KEY,
    actor_id            VARCHAR(2048) NOT NULL UNIQUE,
    inbox_url           VARCHAR(2048) NOT NULL,
    public_key_pem      TEXT,
    public_key_id       VARCHAR(2048),
    profile_fetched_at  TIMESTAMP
);

CREATE TABLE tb_activitypub_follows (
    id                  BIGSERIAL PRIMARY KEY,
    local_actor_id      BIGINT NOT NULL REFERENCES tb_activitypub_actors(id) ON DELETE CASCADE,
    remote_actor_id     BIGINT NOT NULL REFERENCES tb_activitypub_remote_actors(id) ON DELETE CASCADE,
    status              VARCHAR(16) NOT NULL,
    follow_activity_id  VARCHAR(2048),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    accepted_at         TIMESTAMP,
    UNIQUE (local_actor_id, remote_actor_id)
);

CREATE INDEX idx_activitypub_follows_local_status
    ON tb_activitypub_follows (local_actor_id, status);

CREATE TABLE tb_activitypub_deliveries (
    id                  BIGSERIAL PRIMARY KEY,
    local_actor_id      BIGINT NOT NULL REFERENCES tb_activitypub_actors(id) ON DELETE CASCADE,
    activity_type       VARCHAR(16) NOT NULL,
    object_id           VARCHAR(2048) NOT NULL,
    payload_json        TEXT NOT NULL,
    target_inbox_url    VARCHAR(2048) NOT NULL,
    status              VARCHAR(16) NOT NULL,
    attempts            INT NOT NULL DEFAULT 0,
    next_retry_at       TIMESTAMP,
    last_error          TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    delivered_at        TIMESTAMP
);

CREATE INDEX idx_activitypub_deliveries_pending
    ON tb_activitypub_deliveries (status, next_retry_at)
    WHERE status = 'PENDING';
