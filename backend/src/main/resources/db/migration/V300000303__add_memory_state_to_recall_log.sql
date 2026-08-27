ALTER TABLE `recall_log`
  ADD COLUMN `stability_before` FLOAT NULL,
  ADD COLUMN `difficulty_before` FLOAT NULL,
  ADD COLUMN `retrievability` DOUBLE NULL;
