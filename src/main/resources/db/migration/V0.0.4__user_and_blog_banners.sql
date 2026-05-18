ALTER TABLE tb_users
    ADD COLUMN profile_picture_id BIGINT,
    ADD COLUMN default_banner_id BIGINT,
    ADD CONSTRAINT fk_users_profile_picture FOREIGN KEY (profile_picture_id) REFERENCES tb_images(id),
    ADD CONSTRAINT fk_users_default_banner FOREIGN KEY (default_banner_id) REFERENCES tb_images(id);

ALTER TABLE tb_blogs
    ADD COLUMN banner_id BIGINT,
    ADD CONSTRAINT fk_blogs_banner FOREIGN KEY (banner_id) REFERENCES tb_images(id);
