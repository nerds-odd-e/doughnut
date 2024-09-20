ALTER TABLE assessment_question_instance DROP FOREIGN KEY assessment_question_instance_ibfk_2;
ALTER TABLE assessment_question_instance DROP COLUMN review_question_instance_id;

ALTER TABLE assessment_question_instance
ADD COLUMN predefined_question_id INT UNSIGNED;

ALTER TABLE assessment_question_instance
ADD CONSTRAINT fk_assess_question_predefined
FOREIGN KEY (predefined_question_id) REFERENCES predefined_question(id);
