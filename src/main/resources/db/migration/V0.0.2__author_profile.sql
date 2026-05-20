ALTER TABLE tb_users
    ADD COLUMN profile_description TEXT,
    ADD COLUMN website_url VARCHAR(2048),
    ADD COLUMN twitter_url VARCHAR(2048),
    ADD COLUMN mastodon_url VARCHAR(2048),
    ADD COLUMN github_url VARCHAR(2048),
    ADD COLUMN linkedin_url VARCHAR(2048);
