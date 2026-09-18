-- Make the per-notebook title lookup an index hit again. V300000329 added the
-- stored generated column title_uniqueness_key (LOWER(title)); since then MySQL
-- rewrites LOWER(title) predicates to that column, so the functional index on
-- (notebook_id, (lower(title))) no longer matches them and every lookup scanned
-- the notebook. Index the generated column directly under the same index name.
ALTER TABLE note
  DROP INDEX idx_note_notebook_id_title,
  ADD INDEX idx_note_notebook_id_title (notebook_id, title_uniqueness_key);
