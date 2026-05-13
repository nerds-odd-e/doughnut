-- Strict byte identity for PKM titles and names (wikilinks, uniqueness, lookups).
-- Replaces utf8mb4_0900_ai_ci / utf8mb4_unicode_ci equality on these columns.

ALTER TABLE `note`
  DROP INDEX `uk_note_notebook_folder_title`,
  DROP INDEX `idx_note_notebook_id_title`,
  MODIFY `title` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  ADD UNIQUE KEY `uk_note_notebook_folder_title` (
    (ifnull(`notebook_id`, 0)),
    (ifnull(`folder_id`, 0)),
    `title`
  ),
  ADD KEY `idx_note_notebook_id_title` (`notebook_id`, `title`);

ALTER TABLE `notebook`
  DROP INDEX `uk_notebook_ownership_name`,
  MODIFY `name` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  ADD UNIQUE KEY `uk_notebook_ownership_name` (`ownership_id`, `name`);

ALTER TABLE `folder`
  DROP INDEX `uk_folder_notebook_parent_name`,
  MODIFY `name` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  ADD UNIQUE KEY `uk_folder_notebook_parent_name` (
    `notebook_id`,
    (ifnull(`parent_folder_id`, 0)),
    `name`
  );

ALTER TABLE `circle`
  MODIFY `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL;
