CREATE TABLE tb_activitypub_platform_settings (
    id                  INT PRIMARY KEY,
    federation_enabled  BOOLEAN NOT NULL,
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO tb_activitypub_platform_settings (id, federation_enabled, updated_at)
VALUES (1, TRUE, NOW());
