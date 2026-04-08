CREATE TABLE book_range_reading_record (
  id int unsigned NOT NULL AUTO_INCREMENT,
  user_id int unsigned NOT NULL,
  book_range_id int unsigned NOT NULL,
  status varchar(32) NOT NULL,
  completed_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_book_range_reading_user_range (user_id, book_range_id),
  KEY idx_book_range_reading_range (book_range_id),
  CONSTRAINT fk_book_range_reading_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE,
  CONSTRAINT fk_book_range_reading_range FOREIGN KEY (book_range_id) REFERENCES book_range (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
