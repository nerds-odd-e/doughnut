-- First delete all records with check_spell = true
DELETE FROM predefined_question WHERE check_spell = true;

-- Then remove the check_spell column
ALTER TABLE predefined_question DROP COLUMN check_spell;
