UPDATE `note`
INNER JOIN `text_content` ON `note`.`text_content_id` = `text_content`.`id`
SET `note`.`title` = `text_content`.`title`, `note`.`description` = `text_content`.`description`,
`note`.updated_at = `text_content`.updated_at;

