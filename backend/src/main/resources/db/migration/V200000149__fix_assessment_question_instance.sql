-- Drop the existing table if it exists
DROP TABLE IF EXISTS assessment_question_instance;

-- Create the table without the UNIQUE constraint on assessment_attempt_id
CREATE TABLE assessment_question_instance (
    id int unsigned NOT NULL AUTO_INCREMENT,
    assessment_attempt_id int unsigned NOT NULL,
    review_question_instance_id int unsigned NOT NULL UNIQUE,
    PRIMARY KEY(id),
    FOREIGN KEY (assessment_attempt_id) REFERENCES assessment_attempt(id),
    FOREIGN KEY (review_question_instance_id) REFERENCES review_question_instance(id)
);
