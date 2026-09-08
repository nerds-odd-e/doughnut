ALTER TABLE `notebook_git_binding`
  ADD COLUMN `amendment_head` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  ADD COLUMN `amendment_note_id` int unsigned NULL,
  ADD COLUMN `amendment_last_changed_at` timestamp NULL;
