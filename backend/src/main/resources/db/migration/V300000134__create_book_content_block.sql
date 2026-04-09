CREATE TABLE book_content_block (
  id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  book_block_id INT UNSIGNED NOT NULL,
  sibling_order INT NOT NULL,
  type VARCHAR(128) NOT NULL,
  page_idx INT,
  raw_data LONGTEXT NOT NULL,
  CONSTRAINT fk_bcb_book_block FOREIGN KEY (book_block_id) REFERENCES book_block(id)
);
