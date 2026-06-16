ALTER TABLE `question_generation_batch`
  ADD COLUMN `openai_output_file_id` varchar(128) NULL AFTER `submitted_at`,
  ADD COLUMN `openai_error_file_id` varchar(128) NULL AFTER `openai_output_file_id`,
  ADD COLUMN `output_collected_at` timestamp(3) NULL AFTER `openai_error_file_id`;

ALTER TABLE `question_generation_batch_request`
  ADD COLUMN `status` varchar(32) NOT NULL DEFAULT 'PENDING' AFTER `context_seed`,
  ADD COLUMN `raw_success_payload` longtext NULL AFTER `status`,
  ADD COLUMN `raw_error_payload` longtext NULL AFTER `raw_success_payload`,
  ADD COLUMN `error_detail` text NULL AFTER `raw_error_payload`;
