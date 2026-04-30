CREATE TABLE note_wiki_title_cache (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  note_id INT UNSIGNED NOT NULL,
  target_note_id INT UNSIGNED NOT NULL,
  link_text VARCHAR(767) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_note_wiki_title_cache_note_link (note_id, link_text),
  KEY idx_note_wiki_title_cache_note_id (note_id),
  KEY idx_note_wiki_title_cache_target_note_id (target_note_id),
  CONSTRAINT fk_note_wiki_title_cache_note FOREIGN KEY (note_id) REFERENCES note (id) ON DELETE CASCADE,
  CONSTRAINT fk_note_wiki_title_cache_target FOREIGN KEY (target_note_id) REFERENCES note (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
