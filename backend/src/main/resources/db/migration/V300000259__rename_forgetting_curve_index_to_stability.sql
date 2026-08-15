-- Rename memory_tracker.forgetting_curve_index to stability (same numeric scale).
ALTER TABLE `memory_tracker`
  CHANGE COLUMN `forgetting_curve_index` `stability` float NOT NULL DEFAULT '100';
