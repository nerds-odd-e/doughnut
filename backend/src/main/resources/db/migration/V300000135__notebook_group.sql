CREATE TABLE notebook_group (
  id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  ownership_id INT UNSIGNED NOT NULL,
  name VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY fk_notebook_group_ownership (ownership_id),
  CONSTRAINT fk_notebook_group_ownership FOREIGN KEY (ownership_id) REFERENCES ownership (id)
    ON DELETE CASCADE ON UPDATE RESTRICT
);

ALTER TABLE notebook
  ADD COLUMN notebook_group_id INT UNSIGNED DEFAULT NULL,
  ADD CONSTRAINT fk_notebook_notebook_group FOREIGN KEY (notebook_group_id) REFERENCES notebook_group (id)
    ON DELETE SET NULL ON UPDATE RESTRICT;
