ALTER TABLE review_question_instance RENAME TO recall_prompt;
ALTER TABLE conversation
RENAME COLUMN review_question_instance_id TO recall_prompt_id;

