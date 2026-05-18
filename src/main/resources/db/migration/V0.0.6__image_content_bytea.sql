CREATE TABLE tb_image_content (
    image_id BIGINT PRIMARY KEY REFERENCES tb_images(id) ON DELETE CASCADE,
    content  BYTEA NOT NULL
);

ALTER TABLE tb_images DROP COLUMN file_path;
