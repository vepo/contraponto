ALTER TABLE tb_git_sync_runs
    DROP CONSTRAINT chk_git_sync_runs_operation;

ALTER TABLE tb_git_sync_runs
    ADD CONSTRAINT chk_git_sync_runs_operation CHECK (operation IN ('EXPORT', 'IMPORT', 'REPROCESS'));
