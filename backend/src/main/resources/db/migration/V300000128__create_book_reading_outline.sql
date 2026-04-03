CREATE TABLE book_anchor (
  id int unsigned NOT NULL AUTO_INCREMENT,
  anchor_format varchar(255) NOT NULL,
  value text NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE book (
  id int unsigned NOT NULL AUTO_INCREMENT,
  notebook_id int unsigned NOT NULL,
  book_name varchar(512) NOT NULL,
  format varchar(32) NOT NULL,
  source_file_ref varchar(1024) NULL,
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_book_notebook_id (notebook_id),
  CONSTRAINT fk_book_notebook FOREIGN KEY (notebook_id) REFERENCES notebook (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE book_range (
  id int unsigned NOT NULL AUTO_INCREMENT,
  book_id int unsigned NOT NULL,
  parent_range_id int unsigned NULL,
  structural_title varchar(512) NOT NULL,
  start_anchor_id int unsigned NOT NULL,
  end_anchor_id int unsigned NOT NULL,
  sibling_order bigint NOT NULL,
  PRIMARY KEY (id),
  KEY idx_book_range_book_id (book_id),
  KEY idx_book_range_parent_id (parent_range_id),
  CONSTRAINT fk_book_range_book FOREIGN KEY (book_id) REFERENCES book (id) ON DELETE CASCADE,
  CONSTRAINT fk_book_range_parent FOREIGN KEY (parent_range_id) REFERENCES book_range (id) ON DELETE CASCADE,
  CONSTRAINT fk_book_range_start_anchor FOREIGN KEY (start_anchor_id) REFERENCES book_anchor (id),
  CONSTRAINT fk_book_range_end_anchor FOREIGN KEY (end_anchor_id) REFERENCES book_anchor (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
