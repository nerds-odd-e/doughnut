ALTER TABLE conversation
ADD COLUMN review_question_instance_id int unsigned DEFAULT NULL,
ADD FOREIGN KEY
(review_question_instance_id) REFERENCES review_question_instance
(id);
