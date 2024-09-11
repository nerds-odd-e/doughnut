ALTER TABLE `conversation`
ADD COLUMN `quiz_question_id` INT UNSIGNED,
ADD CONSTRAINT `fk_conversation_quiz_question`
FOREIGN KEY (`quiz_question_id`) REFERENCES `quiz_question`(`id`);

-- Step 2: Update quiz_answer to set the new association
UPDATE `conversation` qa
JOIN `question_and_answer` qna ON qa.quiz_question_and_answer_id = qna.id
JOIN `quiz_question` qq ON qq.question_and_answer_id = qna.id
SET qa.quiz_question_id = qq.id;

ALTER TABLE `conversation`
DROP FOREIGN KEY `conversation_ibfk_1`,
DROP COLUMN `quiz_question_and_answer_id`;

