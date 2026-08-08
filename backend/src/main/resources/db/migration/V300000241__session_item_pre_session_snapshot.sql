ALTER TABLE `session_item`
  ADD COLUMN `pre_session_forgetting_curve_index` FLOAT NULL,
  ADD COLUMN `pre_session_recall_count` INT NULL;
