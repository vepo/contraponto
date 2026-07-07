ALTER TABLE tb_activitypub_remote_actors
    ADD COLUMN display_name VARCHAR(512),
    ADD COLUMN preferred_username VARCHAR(255);
