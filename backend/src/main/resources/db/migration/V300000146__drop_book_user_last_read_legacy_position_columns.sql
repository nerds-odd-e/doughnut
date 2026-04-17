-- Track F6: legacy reading-position columns replaced by reading_position_locator_json.
ALTER TABLE book_user_last_read_position
  DROP COLUMN page_index,
  DROP COLUMN normalized_y,
  DROP COLUMN epub_locator;
