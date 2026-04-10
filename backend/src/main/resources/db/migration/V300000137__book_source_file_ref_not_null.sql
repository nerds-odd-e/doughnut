DELETE FROM book
WHERE source_file_ref IS NULL OR TRIM(source_file_ref) = '';

ALTER TABLE book
  MODIFY source_file_ref varchar(1024) NOT NULL;
