ALTER TABLE `memory_tracker`
  ADD COLUMN `difficulty` float DEFAULT NULL;

UPDATE `memory_tracker`
  SET `difficulty` = 5
  WHERE `stability` > 0 OR `recall_count` > 0;
