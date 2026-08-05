ALTER TABLE tb_blogs
    ADD COLUMN git_config_path VARCHAR(2048);

CREATE UNIQUE INDEX ux_blogs_git_remote_config
    ON tb_blogs (owner_id, git_remote_url, COALESCE(git_config_path, ''))
    WHERE git_remote_url IS NOT NULL;
