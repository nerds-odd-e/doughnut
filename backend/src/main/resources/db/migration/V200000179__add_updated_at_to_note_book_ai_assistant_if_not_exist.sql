SELECT IF(
    NOT EXISTS(
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'notebook_ai_assistant'
        AND column_name = 'updated_at'
    ),
    'ALTER TABLE notebook_ai_assistant ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP',
    'SELECT 1'
) INTO @sql;

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
