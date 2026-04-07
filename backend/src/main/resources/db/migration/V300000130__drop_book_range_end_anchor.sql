ALTER TABLE book_range DROP FOREIGN KEY fk_book_range_end_anchor;

DELETE ba FROM book_anchor ba
INNER JOIN book_range br ON br.end_anchor_id = ba.id;

ALTER TABLE book_range DROP COLUMN end_anchor_id;
