ALTER TABLE notebook
  ADD COLUMN short_details VARCHAR(500) NULL;

UPDATE notebook n
INNER JOIN notebook_head_note nh ON nh.notebook_id = n.id
INNER JOIN note hn ON nh.head_note_id = hn.id AND hn.deleted_at IS NULL
SET n.short_details = CASE
  WHEN hn.details IS NULL THEN NULL
  WHEN TRIM(REGEXP_REPLACE(hn.details, '<[^>]*>', '')) = '' THEN NULL
  WHEN CHAR_LENGTH(TRIM(REGEXP_REPLACE(hn.details, '<[^>]*>', ''))) <= 500
    THEN TRIM(REGEXP_REPLACE(hn.details, '<[^>]*>', ''))
  ELSE LEFT(TRIM(REGEXP_REPLACE(hn.details, '<[^>]*>', '')), 500)
END;
