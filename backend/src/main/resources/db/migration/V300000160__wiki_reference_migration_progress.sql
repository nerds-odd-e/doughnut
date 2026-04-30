CREATE TABLE wiki_reference_migration_progress (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  step_name VARCHAR(128) NOT NULL,
  status VARCHAR(32) NOT NULL,
  total_count INT NOT NULL DEFAULT 0,
  processed_count INT NOT NULL DEFAULT 0,
  last_processed_note_id INT UNSIGNED NULL,
  last_error TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  completed_at TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_wiki_reference_migration_progress_step (step_name),
  KEY idx_wiki_reference_migration_progress_status (status),
  CONSTRAINT fk_wiki_ref_mig_progress_last_note
    FOREIGN KEY (last_processed_note_id) REFERENCES note (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
