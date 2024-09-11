ALTER TABLE `quiz_answer`
DROP FOREIGN KEY `fk_quiz_answer_id`;

-- Step 2: Update quiz_answer to set the new association
UPDATE `quiz_answer` qa
JOIN `question_and_answer` qna ON qa.quiz_question_id = qna.id
JOIN `quiz_question` qq ON qq.question_and_answer_id = qna.id
SET qa.quiz_question_id = qq.id;

-- Step 3: Add foreign key constraint to the new column
ALTER TABLE `quiz_answer`
ADD CONSTRAINT `fk_quiz_question`
FOREIGN KEY (`quiz_question_id`) REFERENCES `quiz_question`(`id`);
