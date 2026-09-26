DROP TABLE `attachment_blob`;

ALTER TABLE `book`
  DROP COLUMN `source_file_ref`,
  MODIFY COLUMN `source_file_path` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL;
