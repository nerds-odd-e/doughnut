-- Persist user-level Health run option default: Remove empty folders.
ALTER TABLE `user`
  ADD COLUMN `health_remove_empty_folders_default` tinyint(1) NOT NULL DEFAULT 0;
