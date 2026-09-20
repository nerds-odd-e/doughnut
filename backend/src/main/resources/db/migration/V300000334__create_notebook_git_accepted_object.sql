CREATE TABLE `notebook_git_accepted_object` (
  `notebook_git_binding_id` int unsigned NOT NULL,
  `git_object_id` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `object_type` tinyint unsigned NOT NULL,
  `object_bytes` longblob NOT NULL,
  PRIMARY KEY (`notebook_git_binding_id`, `git_object_id`),
  CONSTRAINT `fk_notebook_git_accepted_object_binding` FOREIGN KEY (`notebook_git_binding_id`) REFERENCES `notebook_git_binding` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_notebook_git_accepted_object_type` CHECK ((`object_type` BETWEEN 1 AND 4))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
