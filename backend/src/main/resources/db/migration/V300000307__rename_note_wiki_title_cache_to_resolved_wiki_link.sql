-- Rename table note_wiki_title_cache to resolved_wiki_link and its columns to
-- source_note_id, destination_note_id, authored_link.

ALTER TABLE `note_wiki_title_cache`
  DROP FOREIGN KEY `fk_note_wiki_title_cache_note`,
  DROP FOREIGN KEY `fk_note_wiki_title_cache_target`;

RENAME TABLE `note_wiki_title_cache` TO `resolved_wiki_link`;

ALTER TABLE `resolved_wiki_link`
  CHANGE COLUMN `note_id` `source_note_id` int unsigned NOT NULL,
  CHANGE COLUMN `target_note_id` `destination_note_id` int unsigned NOT NULL,
  CHANGE COLUMN `link_text` `authored_link` varchar(767) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  RENAME INDEX `uq_note_wiki_title_cache_note_link` TO `uq_resolved_wiki_link_source_authored`,
  RENAME INDEX `idx_note_wiki_title_cache_note_id` TO `idx_resolved_wiki_link_source_note_id`,
  RENAME INDEX `idx_note_wiki_title_cache_target_note_id` TO `idx_resolved_wiki_link_destination_note_id`,
  RENAME INDEX `idx_note_wiki_title_cache_target_note_id_note_id` TO `idx_resolved_wiki_link_destination_source_id`;

ALTER TABLE `resolved_wiki_link`
  ADD CONSTRAINT `fk_resolved_wiki_link_source` FOREIGN KEY (`source_note_id`) REFERENCES `note` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_resolved_wiki_link_destination` FOREIGN KEY (`destination_note_id`) REFERENCES `note` (`id`) ON DELETE CASCADE;
