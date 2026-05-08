-- Wikidata Q-id now lives only in note content YAML frontmatter (see V300000187).
ALTER TABLE `note` DROP COLUMN `wikidata_id`;
