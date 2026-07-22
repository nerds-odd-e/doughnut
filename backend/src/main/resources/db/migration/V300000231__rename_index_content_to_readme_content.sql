-- Rename container-owned markdown from index_content to readme_content.
ALTER TABLE `notebook`
  CHANGE COLUMN `index_content` `readme_content` mediumtext COLLATE utf8mb4_unicode_ci;

ALTER TABLE `folder`
  CHANGE COLUMN `index_content` `readme_content` mediumtext;
