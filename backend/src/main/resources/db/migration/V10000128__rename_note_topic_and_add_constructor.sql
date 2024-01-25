ALTER TABLE note RENAME COLUMN `title` TO `topic_constructor`;
ALTER TABLE note ADD `constructor_id` int unsigned DEFAULT NULL;
ALTER TABLE note ADD FOREIGN KEY (constructor_id) REFERENCES note(id);
