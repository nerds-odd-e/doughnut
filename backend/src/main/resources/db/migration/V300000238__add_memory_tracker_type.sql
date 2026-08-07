ALTER TABLE `memory_tracker`
  ADD COLUMN `type` VARCHAR(32) NOT NULL DEFAULT 'UNDERSTANDING';

UPDATE `memory_tracker` SET `type` = 'SPELLING' WHERE `spelling` = 1;
