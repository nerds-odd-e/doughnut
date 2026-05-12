-- Add container-owned index markdown columns to notebook and folder.
-- Copies unambiguous legacy index note content and renames migrated notes.

ALTER TABLE notebook
  ADD COLUMN index_content MEDIUMTEXT NULL DEFAULT NULL;

ALTER TABLE folder
  ADD COLUMN index_content MEDIUMTEXT NULL DEFAULT NULL;

-- ============================================================
-- Copy legacy notebook index note content (unambiguous only).
-- A notebook index note is unambiguous when there is exactly
-- one non-deleted, root-level (folder_id IS NULL) note titled
-- 'index' (case-insensitive) in that notebook.
-- ============================================================

DROP PROCEDURE IF EXISTS doughnut_migrate_notebook_index_content;
DELIMITER $$
CREATE PROCEDURE doughnut_migrate_notebook_index_content()
BEGIN
  UPDATE notebook nb
  INNER JOIN (
    SELECT notebook_id, MIN(id) AS note_id
    FROM note
    WHERE deleted_at IS NULL
      AND folder_id IS NULL
      AND LOWER(title) = 'index'
    GROUP BY notebook_id
    HAVING COUNT(*) = 1
  ) eligible ON nb.id = eligible.notebook_id
  INNER JOIN note n ON n.id = eligible.note_id
  SET nb.index_content = n.content
  WHERE nb.index_content IS NULL;
END$$
DELIMITER ;

CALL doughnut_migrate_notebook_index_content();
DROP PROCEDURE IF EXISTS doughnut_migrate_notebook_index_content;

-- ============================================================
-- Copy legacy folder index note content (unambiguous only).
-- A folder index note is unambiguous when there is exactly
-- one non-deleted note titled 'index' (case-insensitive)
-- in that folder.
-- ============================================================

DROP PROCEDURE IF EXISTS doughnut_migrate_folder_index_content;
DELIMITER $$
CREATE PROCEDURE doughnut_migrate_folder_index_content()
BEGIN
  UPDATE folder f
  INNER JOIN (
    SELECT folder_id, MIN(id) AS note_id
    FROM note
    WHERE deleted_at IS NULL
      AND folder_id IS NOT NULL
      AND LOWER(title) = 'index'
    GROUP BY folder_id
    HAVING COUNT(*) = 1
  ) eligible ON f.id = eligible.folder_id
  INNER JOIN note n ON n.id = eligible.note_id
  SET f.index_content = n.content
  WHERE f.index_content IS NULL;
END$$
DELIMITER ;

CALL doughnut_migrate_folder_index_content();
DROP PROCEDURE IF EXISTS doughnut_migrate_folder_index_content;

-- ============================================================
-- Rename migrated notebook root index notes to
-- 'index_to_be_deleted'. Uses 'index_to_be_deleted_<id>' when
-- 'index_to_be_deleted' already exists in the same scope
-- (unique key: notebook_id + folder_id + title).
-- Only renames notes from unambiguous notebooks (those that
-- now have index_content set via the migration above).
-- ============================================================

DROP PROCEDURE IF EXISTS doughnut_rename_notebook_index_notes;
DELIMITER $$
CREATE PROCEDURE doughnut_rename_notebook_index_notes()
BEGIN
  DECLARE done INT DEFAULT FALSE;
  DECLARE v_note_id INT;
  DECLARE v_notebook_id INT;
  DECLARE cur CURSOR FOR
    SELECT n.id, n.notebook_id
    FROM note n
    INNER JOIN notebook nb ON nb.id = n.notebook_id
    WHERE n.deleted_at IS NULL
      AND n.folder_id IS NULL
      AND LOWER(n.title) = 'index'
      AND nb.index_content IS NOT NULL;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

  OPEN cur;
  read_loop: LOOP
    FETCH cur INTO v_note_id, v_notebook_id;
    IF done THEN
      LEAVE read_loop;
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM note
      WHERE notebook_id = v_notebook_id
        AND IFNULL(folder_id, 0) = 0
        AND title = 'index_to_be_deleted'
        AND deleted_at IS NULL
    ) THEN
      UPDATE note SET title = 'index_to_be_deleted'
      WHERE id = v_note_id;
    ELSE
      UPDATE note SET title = CONCAT('index_to_be_deleted_', v_note_id)
      WHERE id = v_note_id;
    END IF;
  END LOOP;
  CLOSE cur;
END$$
DELIMITER ;

CALL doughnut_rename_notebook_index_notes();
DROP PROCEDURE IF EXISTS doughnut_rename_notebook_index_notes;

-- ============================================================
-- Rename migrated folder index notes to 'index_to_be_deleted'.
-- Same collision handling as notebook notes above.
-- ============================================================

DROP PROCEDURE IF EXISTS doughnut_rename_folder_index_notes;
DELIMITER $$
CREATE PROCEDURE doughnut_rename_folder_index_notes()
BEGIN
  DECLARE done INT DEFAULT FALSE;
  DECLARE v_note_id INT;
  DECLARE v_notebook_id INT;
  DECLARE v_folder_id INT;
  DECLARE cur CURSOR FOR
    SELECT n.id, n.notebook_id, n.folder_id
    FROM note n
    INNER JOIN folder f ON f.id = n.folder_id
    WHERE n.deleted_at IS NULL
      AND n.folder_id IS NOT NULL
      AND LOWER(n.title) = 'index'
      AND f.index_content IS NOT NULL;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

  OPEN cur;
  read_loop: LOOP
    FETCH cur INTO v_note_id, v_notebook_id, v_folder_id;
    IF done THEN
      LEAVE read_loop;
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM note
      WHERE notebook_id = v_notebook_id
        AND folder_id = v_folder_id
        AND title = 'index_to_be_deleted'
        AND deleted_at IS NULL
    ) THEN
      UPDATE note SET title = 'index_to_be_deleted'
      WHERE id = v_note_id;
    ELSE
      UPDATE note SET title = CONCAT('index_to_be_deleted_', v_note_id)
      WHERE id = v_note_id;
    END IF;
  END LOOP;
  CLOSE cur;
END$$
DELIMITER ;

CALL doughnut_rename_folder_index_notes();
DROP PROCEDURE IF EXISTS doughnut_rename_folder_index_notes;

-- ============================================================
-- Clear stale index_note_id pointers on notebooks/folders
-- whose designated index note was just renamed away from
-- 'index'. The ON DELETE SET NULL FK handles hard deletes,
-- but renames leave the pointer pointing at a now-renamed
-- note. Clear them so the next reconcile finds nothing (the
-- note is still there, just no longer named 'index').
-- ============================================================

UPDATE notebook nb
INNER JOIN note n ON n.id = nb.index_note_id
SET nb.index_note_id = NULL
WHERE LOWER(n.title) NOT LIKE 'index'
  AND n.deleted_at IS NULL;

UPDATE folder f
INNER JOIN note n ON n.id = f.index_note_id
SET f.index_note_id = NULL
WHERE LOWER(n.title) NOT LIKE 'index'
  AND n.deleted_at IS NULL;
