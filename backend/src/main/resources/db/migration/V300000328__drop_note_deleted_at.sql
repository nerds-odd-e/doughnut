-- Drop the retired note.deleted_at column. Legacy soft-deleted notes were migrated into
-- location-based _trash by V300000326, so note availability now follows folder location alone.
-- idx_note_structural_peer included deleted_at as a filter column; recreate it without the
-- dropped column so structural-peer lookups keep their covering index.
--
-- One-time human-authorized exception to migration immutability (Terry Yin, 2026-09-14): this
-- committed migration is revised in place because it has not yet been applied to production. Do
-- not use this as precedent for editing any other committed migration.
--
-- Retry safety: MySQL 8.4 does not support IF [NOT] EXISTS on ALTER TABLE ADD/DROP COLUMN or
-- DROP INDEX (verified empirically against MySQL 8.4.11; that syntax is a MariaDB-only
-- extension), so a bare repeat of the original three ALTER statements fails with error 1091 once
-- the index drop has already committed on a prior, interrupted attempt (reproduced during this
-- change). Branch explicitly on the actual information_schema shape instead, covering every state
-- reachable by interrupting this migration after any one of its statements commits: the original
-- shape, index-dropped-only, index-and-column-dropped, and the fully completed shape (retried only
-- because Flyway had not yet recorded success). Any other shape is unrecognized and fails loudly
-- rather than being silently accepted.
--
-- The guard procedure itself is also a commit boundary: CREATE PROCEDURE and DROP PROCEDURE are
-- each their own DDL auto-commit, so an interruption between CREATE PROCEDURE and CALL, or
-- between a successful CALL and the final DROP PROCEDURE, would otherwise leave a stale procedure
-- behind and make retry fail on "PROCEDURE already exists" before the schema-shape logic ever
-- runs. DROP PROCEDURE IF EXISTS up front closes that gap unconditionally.
DROP PROCEDURE IF EXISTS `_v300000328_drop_note_deleted_at`;

DELIMITER $$

CREATE PROCEDURE `_v300000328_drop_note_deleted_at`()
BEGIN
  DECLARE column_exists INT DEFAULT 0;
  DECLARE index_columns VARCHAR(255) DEFAULT NULL;

  SELECT EXISTS(
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'note' AND column_name = 'deleted_at'
    )
    INTO column_exists;

  SELECT GROUP_CONCAT(column_name ORDER BY seq_in_index)
    INTO index_columns
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'note'
      AND index_name = 'idx_note_structural_peer';

  IF column_exists = 1 AND index_columns = 'notebook_id,folder_id,deleted_at,id' THEN
    -- Original, unmigrated shape.
    ALTER TABLE note DROP INDEX idx_note_structural_peer;
    ALTER TABLE note DROP COLUMN deleted_at;
    ALTER TABLE note ADD INDEX idx_note_structural_peer (notebook_id, folder_id, id);
  ELSEIF column_exists = 1 AND index_columns IS NULL THEN
    -- Interrupted after the index drop committed; column still present.
    ALTER TABLE note DROP COLUMN deleted_at;
    ALTER TABLE note ADD INDEX idx_note_structural_peer (notebook_id, folder_id, id);
  ELSEIF column_exists = 0 AND index_columns IS NULL THEN
    -- Interrupted after the column drop committed; final index not yet recreated.
    ALTER TABLE note ADD INDEX idx_note_structural_peer (notebook_id, folder_id, id);
  ELSEIF column_exists = 0 AND index_columns = 'notebook_id,folder_id,id' THEN
    -- Already fully applied (e.g. Flyway retried after the final ALTER committed but before
    -- success was recorded, or after repair()). Nothing left to do.
    SELECT 1;
  ELSE
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT =
        'V300000328: unrecognized note.deleted_at / idx_note_structural_peer schema state';
  END IF;
END$$

DELIMITER ;

CALL `_v300000328_drop_note_deleted_at`();

DROP PROCEDURE `_v300000328_drop_note_deleted_at`;
