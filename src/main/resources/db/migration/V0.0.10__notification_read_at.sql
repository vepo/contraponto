ALTER TABLE tb_notifications
    ADD COLUMN read_at TIMESTAMP(6);

UPDATE tb_notifications
SET read_at = created_at
WHERE read = TRUE;
