ALTER TABLE quiz_question
ADD COLUMN `image_url` varchar(1024) DEFAULT NULL,
ADD COLUMN `image_mask` varchar(1024) DEFAULT NULL;

ALTER TABLE quiz_question
drop column `has_image`;
