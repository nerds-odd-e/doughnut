CREATE TABLE book_user_last_read_position (
  id int unsigned NOT NULL AUTO_INCREMENT,
  user_id int unsigned NOT NULL,
  book_id int unsigned NOT NULL,
  page_index int NOT NULL,
  normalized_y int NOT NULL,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_book_user_last_read (user_id, book_id),
  KEY idx_book_user_last_read_book (book_id),
  CONSTRAINT fk_book_user_last_read_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE,
  CONSTRAINT fk_book_user_last_read_book FOREIGN KEY (book_id) REFERENCES book (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
