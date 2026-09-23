ALTER TABLE `notebook_git_binding`
  ADD COLUMN `attachment_representation` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'RAW';
