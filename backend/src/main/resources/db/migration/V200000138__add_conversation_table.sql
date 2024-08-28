CREATE TABLE conversation (
    id int unsigned NOT NULL AUTO_INCREMENT,
    quiz_question_and_answer_id int unsigned NOT NULL,
    note_creator_id int unsigned NOT NULL,
    conversation_initiator_id int unsigned NOT NULL,
    message text NOT NULL,
    PRIMARY KEY(id),
    FOREIGN KEY (quiz_question_and_answer_id) REFERENCES quiz_question_and_answer(id),
    FOREIGN KEY (note_creator_id) REFERENCES user(id),
    FOREIGN KEY (conversation_initiator_id) REFERENCES user(id)
);
