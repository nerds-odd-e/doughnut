ALTER TABLE `question_generation_batch`
  ADD COLUMN `openai_input_file_id` varchar(128) NULL AFTER `planned_at`,
  ADD COLUMN `openai_batch_id` varchar(128) NULL AFTER `openai_input_file_id`,
  ADD COLUMN `submitted_at` timestamp(3) NULL AFTER `openai_batch_id`;
