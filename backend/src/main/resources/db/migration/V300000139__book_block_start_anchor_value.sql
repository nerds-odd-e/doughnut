ALTER TABLE book_block ADD COLUMN start_anchor_value TEXT NULL;

UPDATE book_block bb
INNER JOIN book_anchor ba ON bb.start_anchor_id = ba.id
SET bb.start_anchor_value = ba.value;

ALTER TABLE book_block MODIFY COLUMN start_anchor_value TEXT NOT NULL;

ALTER TABLE book_block DROP FOREIGN KEY fk_book_block_start_anchor;

ALTER TABLE book_block DROP COLUMN start_anchor_id;

DROP TABLE book_anchor;
