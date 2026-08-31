ALTER TABLE `failure_report`
  ADD COLUMN `fingerprint` VARCHAR(1024) NULL,
  ADD COLUMN `occurrence_count` INT NOT NULL DEFAULT 1;
