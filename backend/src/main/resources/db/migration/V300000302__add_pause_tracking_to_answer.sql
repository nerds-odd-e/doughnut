ALTER TABLE `answer`
  ADD COLUMN `away_ms` INT NULL,
  ADD COLUMN `away_count` INT NULL,
  ADD COLUMN `detour_ms` INT NULL,
  ADD COLUMN `detour_count` INT NULL,
  ADD COLUMN `idle_ms` INT NULL;
