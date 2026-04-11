ALTER TABLE book_user_last_read_position
  ADD COLUMN selected_book_block_id int unsigned NULL,
  ADD CONSTRAINT fk_book_user_last_read_selected_block
    FOREIGN KEY (selected_book_block_id) REFERENCES book_block (id) ON DELETE SET NULL;
