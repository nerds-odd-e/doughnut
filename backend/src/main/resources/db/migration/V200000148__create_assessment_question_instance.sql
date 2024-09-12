CREATE TABLE assessment_question_instance (
    id int unsigned NOT NULL AUTO_INCREMENT,
    assessment_attempt_id int unsigned NOT NULL UNIQUE,
    review_question_instance_id int unsigned NOT NULL UNIQUE,
    PRIMARY KEY(id),
    FOREIGN KEY (assessment_attempt_id) REFERENCES assessment_attempt(id),
    FOREIGN KEY (review_question_instance_id) REFERENCES review_question_instance(id)
);
