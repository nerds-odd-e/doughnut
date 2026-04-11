ALTER TABLE book_block ADD COLUMN layout_sequence INT NULL;
ALTER TABLE book_block ADD COLUMN depth INT NULL;

UPDATE book_block bb
INNER JOIN (
  WITH RECURSIVE walk AS (
    SELECT
      id,
      book_id,
      0 AS tree_depth,
      CONCAT(LPAD(sibling_order, 10, '0'), '.', LPAD(id, 10, '0')) AS sort_path
    FROM book_block
    WHERE parent_block_id IS NULL

    UNION ALL

    SELECT
      c.id,
      c.book_id,
      p.tree_depth + 1,
      CONCAT(p.sort_path, '.', LPAD(c.sibling_order, 10, '0'), '.', LPAD(c.id, 10, '0'))
    FROM book_block c
    INNER JOIN walk p ON c.parent_block_id = p.id
  )
  SELECT
    id,
    tree_depth AS d,
    ROW_NUMBER() OVER (PARTITION BY book_id ORDER BY sort_path) - 1 AS seq
  FROM walk
) AS calc ON bb.id = calc.id
SET bb.layout_sequence = calc.seq, bb.depth = calc.d;

ALTER TABLE book_block MODIFY COLUMN layout_sequence INT NOT NULL;
ALTER TABLE book_block MODIFY COLUMN depth INT NOT NULL;

ALTER TABLE book_block DROP FOREIGN KEY fk_book_block_parent;

ALTER TABLE book_block DROP INDEX idx_book_block_parent_id;

ALTER TABLE book_block DROP COLUMN parent_block_id;

ALTER TABLE book_block DROP COLUMN sibling_order;
