ALTER TABLE book_range_reading_record DROP FOREIGN KEY fk_book_range_reading_range;
ALTER TABLE book_range_reading_record DROP FOREIGN KEY fk_book_range_reading_user;

ALTER TABLE book_range DROP FOREIGN KEY fk_book_range_parent;
ALTER TABLE book_range DROP FOREIGN KEY fk_book_range_book;
ALTER TABLE book_range DROP FOREIGN KEY fk_book_range_start_anchor;

RENAME TABLE book_range TO book_block;

ALTER TABLE book_block DROP INDEX idx_book_range_parent_id;
ALTER TABLE book_block CHANGE COLUMN parent_range_id parent_block_id int unsigned NULL;

ALTER TABLE book_block DROP INDEX idx_book_range_book_id;
ALTER TABLE book_block ADD INDEX idx_book_block_book_id (book_id);
ALTER TABLE book_block ADD INDEX idx_book_block_parent_id (parent_block_id);

ALTER TABLE book_block ADD CONSTRAINT fk_book_block_book FOREIGN KEY (book_id) REFERENCES book (id) ON DELETE CASCADE;
ALTER TABLE book_block ADD CONSTRAINT fk_book_block_parent FOREIGN KEY (parent_block_id) REFERENCES book_block (id) ON DELETE CASCADE;
ALTER TABLE book_block ADD CONSTRAINT fk_book_block_start_anchor FOREIGN KEY (start_anchor_id) REFERENCES book_anchor (id);

ALTER TABLE book_range_reading_record CHANGE COLUMN book_range_id book_block_id int unsigned NOT NULL;

RENAME TABLE book_range_reading_record TO book_block_reading_record;

ALTER TABLE book_block_reading_record DROP INDEX uq_book_range_reading_user_range;
ALTER TABLE book_block_reading_record DROP INDEX idx_book_range_reading_range;

ALTER TABLE book_block_reading_record ADD UNIQUE KEY uq_book_block_reading_user_block (user_id, book_block_id);
ALTER TABLE book_block_reading_record ADD KEY idx_book_block_reading_block (book_block_id);

ALTER TABLE book_block_reading_record ADD CONSTRAINT fk_book_block_reading_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE;
ALTER TABLE book_block_reading_record ADD CONSTRAINT fk_book_block_reading_block FOREIGN KEY (book_block_id) REFERENCES book_block (id) ON DELETE CASCADE;
