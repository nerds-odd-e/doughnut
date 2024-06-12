-- Add the number_of_questions column with specified properties
ALTER TABLE notebook
ADD COLUMN number_of_questions INT UNSIGNED DEFAULT 5;
