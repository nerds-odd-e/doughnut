-- Step 1: Update the correct_answer_index field from the old raw_json_question
UPDATE quiz_question
SET correct_answer_index = JSON_UNQUOTE(JSON_EXTRACT(raw_json_question, '$.correctChoiceIndex'))
WHERE raw_json_question IS NOT NULL
  AND JSON_EXTRACT(raw_json_question, '$.correctChoiceIndex') IS NOT NULL;

-- Step 2: Remove the correctChoiceIndex from the raw_json_question
UPDATE quiz_question
SET raw_json_question = JSON_REMOVE(raw_json_question, '$.correctChoiceIndex')
WHERE raw_json_question IS NOT NULL
  AND JSON_EXTRACT(raw_json_question, '$.correctChoiceIndex') IS NOT NULL;

DELETE FROM quiz_question
WHERE raw_json_question IS NULL OR TRIM(raw_json_question) = '';
