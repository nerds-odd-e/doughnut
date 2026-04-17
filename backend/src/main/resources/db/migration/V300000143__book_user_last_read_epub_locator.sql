ALTER TABLE book_user_last_read_position
  ADD COLUMN epub_locator VARCHAR(512) NULL,
  MODIFY COLUMN page_index int NULL,
  MODIFY COLUMN normalized_y int NULL;
