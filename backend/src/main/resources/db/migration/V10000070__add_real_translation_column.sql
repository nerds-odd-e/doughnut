ALTER TABLE note ADD translation_text_content_id int unsigned DEFAULT NULL;
ALTER TABLE note ADD FOREIGN KEY (translation_text_content_id) REFERENCES text_content(id) ON DELETE CASCADE;