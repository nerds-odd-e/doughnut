-- Notebook-root files projected from accepted Portable Git content. The filename column uses a
-- binary collation so SQL uniqueness matches Git path identity: `Diagram.png` and `diagram.png`
-- are two distinct Git paths and must stay two distinct rows.
CREATE TABLE `notebook_attachment` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `notebook_id` int unsigned NOT NULL,
  `filename` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `content` longblob NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notebook_attachment_notebook_filename` (`notebook_id`, `filename`),
  CONSTRAINT `fk_notebook_attachment_notebook` FOREIGN KEY (`notebook_id`) REFERENCES `notebook` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
