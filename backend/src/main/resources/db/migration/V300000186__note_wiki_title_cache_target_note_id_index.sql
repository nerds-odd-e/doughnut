-- Supports capped inbound-referrer lookups: filter by target, dedupe by referrer note_id, order by id.
CREATE INDEX `idx_note_wiki_title_cache_target_note_id_note_id`
ON `note_wiki_title_cache` (`target_note_id`, `note_id`, `id`);
