RENAME TABLE `question_and_answer` TO `predefined_question`;

ALTER TABLE `quiz_question`
RENAME COLUMN `question_and_answer_id` TO `predefined_question_id`;
