ALTER TABLE review_setting ADD `skip_review` tinyint NOT NULL DEFAULT '0';
UPDATE `review_setting`
INNER JOIN `note` ON `review_setting`.`id` = `note`.`master_review_setting_id`
SET `review_setting`.`skip_review` = `note`.`skip_review`;

