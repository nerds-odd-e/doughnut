-- Add a new column to the review_question_instance table
ALTER TABLE review_question_instance ADD COLUMN quiz_answer_id INT UNSIGNED;

-- Update the existing data to set the quiz_answer_id in review_question_instance
UPDATE review_question_instance rqi
JOIN quiz_answer qa ON qa.review_question_instance_id = rqi.id
SET rqi.quiz_answer_id = qa.id;

-- Remove the foreign key and column from the quiz_answer table
ALTER TABLE quiz_answer DROP FOREIGN KEY fk_quiz_question;
ALTER TABLE quiz_answer DROP COLUMN review_question_instance_id;

-- Add a foreign key constraint to the review_question_instance table
ALTER TABLE review_question_instance
ADD CONSTRAINT fk_quiz_answer
FOREIGN KEY (quiz_answer_id) REFERENCES quiz_answer(id);

ALTER TABLE assessment_question_instance
ADD COLUMN quiz_answer_id INT UNSIGNED;

ALTER TABLE assessment_question_instance
ADD CONSTRAINT fk_assess_question_quiz_answer
FOREIGN KEY (quiz_answer_id) REFERENCES quiz_answer(id);
