-- A New tracker has no last recall; last_recalled_at is last mapped grade.
ALTER TABLE `memory_tracker`
  MODIFY COLUMN `last_recalled_at` datetime NULL;
