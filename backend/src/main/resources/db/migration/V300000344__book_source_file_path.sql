ALTER TABLE `book`
  ADD COLUMN `source_file_path` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  MODIFY COLUMN `source_file_ref` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL;
