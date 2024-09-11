RENAME TABLE `quiz_question_and_answer` TO `question_and_answer`;

ALTER TABLE `question_and_answer`
ADD COLUMN `raw_json_question` text,
ADD COLUMN `image_url` varchar(1024) DEFAULT NULL,
ADD COLUMN `image_mask` varchar(1024) DEFAULT NULL,
ADD COLUMN check_spell BOOLEAN DEFAULT FALSE;

UPDATE question_and_answer qqa
JOIN quiz_question qq ON qqa.quiz_question_id = qq.id
SET qqa.raw_json_question = qq.raw_json_question,
    qqa.image_url = qq.image_url,
    qqa.image_mask = qq.image_mask,
    qqa.check_spell = qq.check_spell;

ALTER TABLE `quiz_question`
ADD COLUMN `question_and_answer_id` int unsigned;

UPDATE `quiz_question` qq
JOIN `question_and_answer` qqa ON qq.id = qqa.quiz_question_id
SET qq.question_and_answer_id = qqa.id;

ALTER TABLE `quiz_question`
ADD CONSTRAINT `fk_question_and_answer`
FOREIGN KEY (`question_and_answer_id`) REFERENCES `question_and_answer`(`id`);

ALTER TABLE `question_and_answer`
DROP FOREIGN KEY `fk_quiz_question_and_answer_quiz_question_id`,
DROP COLUMN `quiz_question_id`;

ALTER TABLE `quiz_question`
DROP COLUMN `raw_json_question`,
DROP COLUMN `image_url`,
DROP COLUMN `image_mask`,
DROP COLUMN `check_spell`;

