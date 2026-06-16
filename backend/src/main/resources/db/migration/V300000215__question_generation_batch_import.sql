ALTER TABLE `question_generation_batch`
  ADD COLUMN `imported_at` timestamp(3) NULL AFTER `output_collected_at`;
