CREATE TABLE folder (
  id int unsigned NOT NULL AUTO_INCREMENT,
  notebook_id int unsigned NOT NULL,
  parent_folder_id int unsigned NULL,
  name varchar(512) NOT NULL,
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_folder_notebook_id (notebook_id),
  KEY idx_folder_parent_folder_id (parent_folder_id),
  CONSTRAINT fk_folder_notebook FOREIGN KEY (notebook_id) REFERENCES notebook (id) ON DELETE CASCADE,
  CONSTRAINT fk_folder_parent FOREIGN KEY (parent_folder_id) REFERENCES folder (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
