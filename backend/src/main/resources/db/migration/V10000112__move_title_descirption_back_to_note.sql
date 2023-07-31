ALTER TABLE note ADD `title` varchar(100) NOT NULL;
ALTER TABLE note ADD `description` text;

UPDATE `note`
INNER JOIN `text_content` ON `note`.`id` = `text_content`.`note_id`
SET `note`.`title` = `text_content`.`title`, `note`.`description` = `text_content`.`description`,
`note`.updated_at = `text_content`.updated_at;
