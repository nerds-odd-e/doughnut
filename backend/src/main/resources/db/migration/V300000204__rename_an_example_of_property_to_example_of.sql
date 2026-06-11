-- Renames frontmatter property keys in the "an example of" family to "example of"
-- ("an example of" -> "example of", "an example of 2" -> "example of 2", …), bumping
-- the suffix when the target slot is already taken. Updates matching property trackers.

DROP PROCEDURE IF EXISTS doughnut_rename_an_example_of_property_keys;
DELIMITER $$
CREATE PROCEDURE doughnut_rename_an_example_of_property_keys()
BEGIN
  DECLARE done INT DEFAULT FALSE;
  DECLARE v_note_id INT UNSIGNED;
  DECLARE v_content MEDIUMTEXT;
  DECLARE v_norm MEDIUMTEXT;
  DECLARE v_frontmatter MEDIUMTEXT;
  DECLARE v_close_pos INT;
  DECLARE v_slot INT;
  DECLARE v_old_key VARCHAR(255);
  DECLARE v_new_key VARCHAR(255);
  DECLARE v_candidate VARCHAR(255);
  DECLARE v_key_found INT;
  DECLARE v_new_content MEDIUMTEXT;

  DECLARE cur CURSOR FOR
    SELECT id, content
    FROM note
    WHERE deleted_at IS NULL
      AND content LIKE '%an example of%'
    ORDER BY id ASC;

  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

  OPEN cur;
  note_loop: LOOP
    FETCH cur INTO v_note_id, v_content;
    IF done THEN
      LEAVE note_loop;
    END IF;
    SET done = FALSE;

    SET v_norm = REPLACE(
      REPLACE(
        IF(
          LENGTH(COALESCE(v_content, '')) >= 3
            AND HEX(LEFT(COALESCE(v_content, ''), 1)) = 'EFBBBF',
          SUBSTRING(COALESCE(v_content, ''), 2),
          COALESCE(v_content, '')
        ),
        CONCAT(CHAR(13), CHAR(10)),
        CHAR(10)
      ),
      CHAR(13),
      CHAR(10)
    );

    IF SUBSTRING_INDEX(v_norm, CHAR(10), 1) <> '---' THEN
      ITERATE note_loop;
    END IF;

    SET v_close_pos = LOCATE(CONCAT(CHAR(10), '---', CHAR(10)), SUBSTRING(v_norm, 5));
    IF v_close_pos = 0 THEN
      ITERATE note_loop;
    END IF;

    SET v_frontmatter = SUBSTRING(SUBSTRING(v_norm, 5), 1, v_close_pos - 1);
    IF LOWER(v_frontmatter) NOT LIKE '%an example of%' THEN
      ITERATE note_loop;
    END IF;

    SET v_new_content = v_norm;
    SET v_slot = 1;

    rename_slot_loop: WHILE v_slot <= 100 DO
      IF v_slot = 1 THEN
        SET v_old_key = 'an example of';
      ELSE
        SET v_old_key = CONCAT('an example of ', v_slot);
      END IF;

      SET v_key_found = (
        LOCATE(
          CONCAT(CHAR(10), LOWER(v_old_key), ':'),
          CONCAT(CHAR(10), LOWER(v_frontmatter), CHAR(10))
        ) > 0
        OR LEFT(LTRIM(v_frontmatter), LENGTH(v_old_key) + 1) = CONCAT(v_old_key, ':')
        OR LEFT(LTRIM(LOWER(v_frontmatter)), LENGTH(v_old_key) + 1) = CONCAT(LOWER(v_old_key), ':')
      );

      IF NOT v_key_found THEN
        SET v_slot = v_slot + 1;
        ITERATE rename_slot_loop;
      END IF;

      SET v_new_key = NULL;
      SET @target_slot = 1;
      find_target: LOOP
        IF @target_slot = 1 THEN
          SET v_candidate = 'example of';
        ELSE
          SET v_candidate = CONCAT('example of ', @target_slot);
        END IF;

        SET @occupied = (
          LOCATE(
            CONCAT(CHAR(10), LOWER(v_candidate), ':'),
            CONCAT(CHAR(10), LOWER(v_frontmatter), CHAR(10))
          ) > 0
          OR LEFT(LTRIM(v_frontmatter), LENGTH(v_candidate) + 1) = CONCAT(v_candidate, ':')
          OR LEFT(LTRIM(LOWER(v_frontmatter)), LENGTH(v_candidate) + 1) = CONCAT(LOWER(v_candidate), ':')
        );

        IF NOT @occupied THEN
          SET v_new_key = v_candidate;
          LEAVE find_target;
        END IF;

        SET @target_slot = @target_slot + 1;
        IF @target_slot > 100 THEN
          SET v_slot = v_slot + 1;
          ITERATE rename_slot_loop;
        END IF;
      END LOOP find_target;

      IF v_new_key IS NULL THEN
        SET v_slot = v_slot + 1;
        ITERATE rename_slot_loop;
      END IF;

      IF LOCATE(CONCAT(CHAR(10), v_old_key, ':'), v_new_content) > 0 THEN
        SET v_new_content = REPLACE(
          v_new_content,
          CONCAT(CHAR(10), v_old_key, ':'),
          CONCAT(CHAR(10), v_new_key, ':')
        );
      END IF;
      IF LEFT(SUBSTRING(v_new_content, 5), LENGTH(v_old_key) + 1) = CONCAT(v_old_key, ':') THEN
        SET v_new_content = REPLACE(
          v_new_content,
          CONCAT('---', CHAR(10), v_old_key, ':'),
          CONCAT('---', CHAR(10), v_new_key, ':')
        );
      END IF;

      SET v_frontmatter = REPLACE(
        REPLACE(
          v_frontmatter,
          CONCAT(CHAR(10), v_old_key, ':'),
          CONCAT(CHAR(10), v_new_key, ':')
        ),
        CONCAT(v_old_key, ':'),
        CONCAT(v_new_key, ':')
      );

      UPDATE memory_tracker mt
      SET mt.property_key = (v_new_key COLLATE utf8mb4_0900_ai_ci)
      WHERE mt.note_id = v_note_id
        AND mt.property_key = (v_old_key COLLATE utf8mb4_0900_ai_ci)
        AND NOT EXISTS (
          SELECT 1
          FROM (
            SELECT id, user_id, note_id, spelling, property_key, deleted_at, removed_from_tracking
            FROM memory_tracker
          ) existing
          WHERE existing.user_id = mt.user_id
            AND existing.note_id = mt.note_id
            AND existing.spelling = mt.spelling
            AND existing.property_key = (v_new_key COLLATE utf8mb4_0900_ai_ci)
            AND existing.deleted_at IS NULL
            AND existing.removed_from_tracking = 0
            AND existing.id <> mt.id
        );

      SET v_slot = v_slot + 1;
    END WHILE rename_slot_loop;

    IF v_new_content <> v_norm THEN
      UPDATE note
      SET content = v_new_content,
          updated_at = UTC_TIMESTAMP()
      WHERE id = v_note_id;
    END IF;
  END LOOP;
  CLOSE cur;
END$$
DELIMITER ;

CALL doughnut_rename_an_example_of_property_keys();
DROP PROCEDURE IF EXISTS doughnut_rename_an_example_of_property_keys;

-- Re-home any remaining property trackers whose key still uses the old base name.
DROP PROCEDURE IF EXISTS doughnut_rename_an_example_of_tracker_keys;
DELIMITER $$
CREATE PROCEDURE doughnut_rename_an_example_of_tracker_keys()
BEGIN
  DECLARE done INT DEFAULT FALSE;
  DECLARE v_tracker_id INT UNSIGNED;
  DECLARE v_note_id INT UNSIGNED;
  DECLARE v_user_id INT UNSIGNED;
  DECLARE v_spelling TINYINT;
  DECLARE v_old_key VARCHAR(255);
  DECLARE v_new_key VARCHAR(255);
  DECLARE v_candidate VARCHAR(255);
  DECLARE v_frontmatter MEDIUMTEXT;
  DECLARE v_content MEDIUMTEXT;
  DECLARE v_norm MEDIUMTEXT;
  DECLARE v_close_pos INT;
  DECLARE v_slot INT;
  DECLARE v_key_found INT;

  DECLARE cur CURSOR FOR
    SELECT mt.id, mt.note_id, mt.user_id, mt.spelling, mt.property_key
    FROM memory_tracker mt
    WHERE mt.deleted_at IS NULL
      AND mt.removed_from_tracking = 0
      AND mt.property_key <> ''
      AND (
        LOWER(mt.property_key) = 'an example of'
        OR LOWER(mt.property_key) LIKE 'an example of %'
      )
    ORDER BY mt.note_id ASC, mt.id ASC;

  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

  OPEN cur;
  tracker_loop: LOOP
    FETCH cur INTO v_tracker_id, v_note_id, v_user_id, v_spelling, v_old_key;
    IF done THEN
      LEAVE tracker_loop;
    END IF;
    SET done = FALSE;

    SET v_content = (
      SELECT content
      FROM note
      WHERE id = v_note_id
    );

    SET v_norm = REPLACE(
      REPLACE(
        IF(
          LENGTH(COALESCE(v_content, '')) >= 3
            AND HEX(LEFT(COALESCE(v_content, ''), 1)) = 'EFBBBF',
          SUBSTRING(COALESCE(v_content, ''), 2),
          COALESCE(v_content, '')
        ),
        CONCAT(CHAR(13), CHAR(10)),
        CHAR(10)
      ),
      CHAR(13),
      CHAR(10)
    );

    IF SUBSTRING_INDEX(v_norm, CHAR(10), 1) = '---' THEN
      SET v_close_pos = LOCATE(CONCAT(CHAR(10), '---', CHAR(10)), SUBSTRING(v_norm, 5));
      IF v_close_pos > 0 THEN
        SET v_frontmatter = SUBSTRING(SUBSTRING(v_norm, 5), 1, v_close_pos - 1);
      ELSE
        SET v_frontmatter = '';
      END IF;
    ELSE
      SET v_frontmatter = '';
    END IF;

    SET v_new_key = NULL;
    SET v_slot = 1;
    find_target: LOOP
      IF v_slot = 1 THEN
        SET v_candidate = 'example of';
      ELSE
        SET v_candidate = CONCAT('example of ', v_slot);
      END IF;

      SET v_key_found = (
        LOCATE(
          CONCAT(CHAR(10), LOWER(v_candidate), ':'),
          CONCAT(CHAR(10), LOWER(v_frontmatter), CHAR(10))
        ) > 0
        OR LEFT(LTRIM(v_frontmatter), LENGTH(v_candidate) + 1) = CONCAT(v_candidate, ':')
        OR LEFT(LTRIM(LOWER(v_frontmatter)), LENGTH(v_candidate) + 1) = CONCAT(LOWER(v_candidate), ':')
      );

      IF NOT v_key_found
        AND NOT EXISTS (
          SELECT 1
          FROM memory_tracker existing
          WHERE existing.user_id = v_user_id
            AND existing.note_id = v_note_id
            AND existing.spelling = v_spelling
            AND existing.property_key = (v_candidate COLLATE utf8mb4_0900_ai_ci)
            AND existing.deleted_at IS NULL
            AND existing.removed_from_tracking = 0
            AND existing.id <> v_tracker_id
        ) THEN
        SET v_new_key = v_candidate;
        LEAVE find_target;
      END IF;

      SET v_slot = v_slot + 1;
      IF v_slot > 100 THEN
        LEAVE tracker_loop;
      END IF;
    END LOOP find_target;

    IF v_new_key IS NOT NULL THEN
      UPDATE memory_tracker
      SET property_key = (v_new_key COLLATE utf8mb4_0900_ai_ci)
      WHERE id = v_tracker_id;
    END IF;
  END LOOP;
  CLOSE cur;
END$$
DELIMITER ;

CALL doughnut_rename_an_example_of_tracker_keys();
DROP PROCEDURE IF EXISTS doughnut_rename_an_example_of_tracker_keys;
