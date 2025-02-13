-- First delete dependent records in recall_prompt table
DELETE FROM recall_prompt
WHERE predefined_question_id IN (
    SELECT id FROM predefined_question WHERE check_spell = true
);

DELETE FROM assessment_question_instance
WHERE predefined_question_id IN (
    SELECT id FROM predefined_question WHERE check_spell = true
);
-- Then delete records from predefined_question
DELETE FROM predefined_question WHERE check_spell = true;

-- Finally remove the check_spell column
ALTER TABLE predefined_question DROP COLUMN check_spell;
