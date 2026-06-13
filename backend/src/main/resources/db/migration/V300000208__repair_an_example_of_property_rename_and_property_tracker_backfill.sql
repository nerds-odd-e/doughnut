-- Repairs missed "an example of" → "example of" renames (robust line-based frontmatter rewrite),
-- re-homes orphan property trackers, drops stale index rows, and seeds skipped trackers from
-- note_property_index for notebook owners.

DROP PROCEDURE IF EXISTS doughnut_repair_an_example_of_property_keys;
DELIMITER $$
CREATE PROCEDURE doughnut_repair_an_example_of_property_keys()
BEGIN
  DECLARE done INT DEFAULT FALSE;
  DECLARE v_note_id INT UNSIGNED;
  DECLARE v_content MEDIUMTEXT;
  DECLARE v_norm MEDIUMTEXT;
  DECLARE v_after_open MEDIUMTEXT;
  DECLARE v_close_pos INT;
  DECLARE v_frontmatter MEDIUMTEXT;
  DECLARE v_body MEDIUMTEXT;
  DECLARE v_new_frontmatter MEDIUMTEXT;
  DECLARE v_rest MEDIUMTEXT;
  DECLARE v_line MEDIUMTEXT;
  DECLARE v_line_pos INT;
  DECLARE v_colon_pos INT;
  DECLARE v_key_part VARCHAR(255);
  DECLARE v_key_lower VARCHAR(255);
  DECLARE v_suffix INT;
  DECLARE v_old_slot INT;
  DECLARE v_new_key VARCHAR(255);
  DECLARE v_candidate VARCHAR(255);
  DECLARE v_target_slot INT;
  DECLARE v_occupied INT;
  DECLARE v_new_content MEDIUMTEXT;
  DECLARE v_changed TINYINT DEFAULT 0;

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

    SET v_after_open = SUBSTRING(v_norm, 5);
    SET v_close_pos = LOCATE(CONCAT(CHAR(10), '---', CHAR(10)), v_after_open);
    SET v_body = '';

    IF v_close_pos > 0 THEN
      SET v_frontmatter = SUBSTRING(v_after_open, 1, v_close_pos - 1);
      SET v_body = SUBSTRING(v_after_open, v_close_pos + 5);
    ELSE
      SET v_close_pos = LOCATE(CONCAT(CHAR(10), '---'), v_after_open);
      IF v_close_pos > 0
        AND (
          v_close_pos + 4 = LENGTH(v_after_open)
          OR SUBSTRING(v_after_open, v_close_pos + 4) = ''
        ) THEN
        SET v_frontmatter = SUBSTRING(v_after_open, 1, v_close_pos - 1);
      ELSE
        ITERATE note_loop;
      END IF;
    END IF;

    IF LOWER(v_frontmatter) NOT LIKE '%an example of%' THEN
      ITERATE note_loop;
    END IF;

    SET v_new_frontmatter = '';
    SET v_rest = v_frontmatter;
    SET v_changed = 0;

    line_loop: WHILE LENGTH(v_rest) > 0 DO
      SET v_line_pos = LOCATE(CHAR(10), v_rest);
      IF v_line_pos = 0 THEN
        SET v_line = v_rest;
        SET v_rest = '';
      ELSE
        SET v_line = LEFT(v_rest, v_line_pos - 1);
        SET v_rest = SUBSTRING(v_rest, v_line_pos + 1);
      END IF;

      IF TRIM(v_line) = '' THEN
        IF LENGTH(v_new_frontmatter) > 0 THEN
          SET v_new_frontmatter = CONCAT(v_new_frontmatter, CHAR(10));
        END IF;
        ITERATE line_loop;
      END IF;

      SET v_colon_pos = LOCATE(':', v_line);
      IF v_colon_pos = 0 THEN
        SET v_new_frontmatter = CONCAT(v_new_frontmatter, v_line, CHAR(10));
        ITERATE line_loop;
      END IF;

      SET v_key_part = TRIM(
        BOTH '"' FROM TRIM(
          BOTH '''' FROM TRIM(LEFT(v_line, v_colon_pos - 1))
        )
      );
      SET v_key_lower = LOWER(v_key_part);

      SET v_old_slot = NULL;
      IF v_key_lower = 'an example of' THEN
        SET v_old_slot = 1;
      ELSEIF v_key_lower LIKE 'an example of %' THEN
        SET v_suffix = CAST(TRIM(SUBSTRING(v_key_lower, 15)) AS UNSIGNED);
        IF v_suffix >= 2 THEN
          SET v_old_slot = v_suffix;
        END IF;
      END IF;

      IF v_old_slot IS NULL THEN
        SET v_new_frontmatter = CONCAT(v_new_frontmatter, v_line, CHAR(10));
        ITERATE line_loop;
      END IF;

      SET v_new_key = NULL;
      SET v_target_slot = 1;
      find_target: LOOP
        IF v_target_slot = 1 THEN
          SET v_candidate = 'example of';
        ELSE
          SET v_candidate = CONCAT('example of ', v_target_slot);
        END IF;

        SET v_occupied = (
          LOCATE(
            CONCAT(CHAR(10), LOWER(v_candidate), ':'),
            CONCAT(CHAR(10), LOWER(CONCAT(v_new_frontmatter, v_rest)), CHAR(10))
          ) > 0
          OR LOCATE(
            CONCAT(CHAR(10), LOWER(v_candidate), ' :'),
            CONCAT(CHAR(10), LOWER(CONCAT(v_new_frontmatter, v_rest)), CHAR(10))
          ) > 0
          OR (
            LENGTH(v_new_frontmatter) = 0
            AND (
              LEFT(LTRIM(LOWER(v_rest)), LENGTH(v_candidate) + 1) = CONCAT(LOWER(v_candidate), ':')
              OR LEFT(LTRIM(LOWER(v_rest)), LENGTH(v_candidate) + 2) = CONCAT(LOWER(v_candidate), ' :')
            )
          )
          OR LOCATE(
            CONCAT(CHAR(10), CONCAT('"', LOWER(v_candidate), '"'), ':'),
            CONCAT(CHAR(10), LOWER(CONCAT(v_new_frontmatter, v_rest)), CHAR(10))
          ) > 0
          OR LOCATE(
            CONCAT(CHAR(10), CONCAT('''', LOWER(v_candidate), ''''), ':'),
            CONCAT(CHAR(10), LOWER(CONCAT(v_new_frontmatter, v_rest)), CHAR(10))
          ) > 0
        );

        IF NOT v_occupied THEN
          SET v_new_key = v_candidate;
          LEAVE find_target;
        END IF;

        SET v_target_slot = v_target_slot + 1;
        IF v_target_slot > 100 THEN
          SET v_new_key = NULL;
          LEAVE find_target;
        END IF;
      END LOOP find_target;

      IF v_new_key IS NULL THEN
        SET v_new_frontmatter = CONCAT(v_new_frontmatter, v_line, CHAR(10));
        ITERATE line_loop;
      END IF;

      SET v_line = CONCAT(v_new_key, SUBSTRING(v_line, v_colon_pos));
      SET v_new_frontmatter = CONCAT(v_new_frontmatter, v_line, CHAR(10));
      SET v_changed = 1;

      UPDATE memory_tracker mt
      SET mt.property_key = (v_new_key COLLATE utf8mb4_0900_ai_ci)
      WHERE mt.note_id = v_note_id
        AND mt.property_key = (v_key_part COLLATE utf8mb4_0900_ai_ci)
        AND mt.deleted_at IS NULL
        AND NOT EXISTS (
          SELECT 1
          FROM (
            SELECT id, user_id, note_id, spelling, property_key, deleted_at
            FROM memory_tracker
          ) existing
          WHERE existing.user_id = mt.user_id
            AND existing.note_id = mt.note_id
            AND existing.spelling = mt.spelling
            AND existing.property_key = (v_new_key COLLATE utf8mb4_0900_ai_ci)
            AND existing.deleted_at IS NULL
            AND existing.id <> mt.id
        );
    END WHILE line_loop;

    IF v_changed = 0 THEN
      ITERATE note_loop;
    END IF;

    IF RIGHT(v_new_frontmatter, 1) = CHAR(10) THEN
      SET v_new_frontmatter = LEFT(v_new_frontmatter, LENGTH(v_new_frontmatter) - 1);
    END IF;

    SET v_new_content = CONCAT('---', CHAR(10), v_new_frontmatter, CHAR(10), '---', CHAR(10), v_body);

    UPDATE note
    SET content = v_new_content,
        updated_at = UTC_TIMESTAMP()
    WHERE id = v_note_id;
  END LOOP;
  CLOSE cur;
END$$
DELIMITER ;

CALL doughnut_repair_an_example_of_property_keys();
DROP PROCEDURE IF EXISTS doughnut_repair_an_example_of_property_keys;

DROP PROCEDURE IF EXISTS doughnut_repair_an_example_of_tracker_keys;
DELIMITER $$
CREATE PROCEDURE doughnut_repair_an_example_of_tracker_keys()
BEGIN
  DECLARE done INT DEFAULT FALSE;
  DECLARE v_tracker_id INT UNSIGNED;
  DECLARE v_note_id INT UNSIGNED;
  DECLARE v_user_id INT UNSIGNED;
  DECLARE v_spelling TINYINT;
  DECLARE v_old_key VARCHAR(255);
  DECLARE v_new_key VARCHAR(255);
  DECLARE v_candidate VARCHAR(255);
  DECLARE v_content MEDIUMTEXT;
  DECLARE v_norm MEDIUMTEXT;
  DECLARE v_after_open MEDIUMTEXT;
  DECLARE v_close_pos INT;
  DECLARE v_frontmatter MEDIUMTEXT;
  DECLARE v_slot INT;
  DECLARE v_key_found INT;

  DECLARE cur CURSOR FOR
    SELECT mt.id, mt.note_id, mt.user_id, mt.spelling, mt.property_key
    FROM memory_tracker mt
    WHERE mt.deleted_at IS NULL
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

    SET v_frontmatter = '';
    IF SUBSTRING_INDEX(v_norm, CHAR(10), 1) = '---' THEN
      SET v_after_open = SUBSTRING(v_norm, 5);
      SET v_close_pos = LOCATE(CONCAT(CHAR(10), '---', CHAR(10)), v_after_open);
      IF v_close_pos > 0 THEN
        SET v_frontmatter = SUBSTRING(v_after_open, 1, v_close_pos - 1);
      ELSE
        SET v_close_pos = LOCATE(CONCAT(CHAR(10), '---'), v_after_open);
        IF v_close_pos > 0
          AND (
            v_close_pos + 4 = LENGTH(v_after_open)
            OR SUBSTRING(v_after_open, v_close_pos + 4) = ''
          ) THEN
          SET v_frontmatter = SUBSTRING(v_after_open, 1, v_close_pos - 1);
        END IF;
      END IF;
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
        OR LEFT(LTRIM(LOWER(v_frontmatter)), LENGTH(v_candidate) + 1) = CONCAT(LOWER(v_candidate), ':')
      );

      IF v_key_found
        AND NOT EXISTS (
          SELECT 1
          FROM memory_tracker existing
          WHERE existing.user_id = v_user_id
            AND existing.note_id = v_note_id
            AND existing.spelling = v_spelling
            AND existing.property_key = (v_candidate COLLATE utf8mb4_0900_ai_ci)
            AND existing.deleted_at IS NULL
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

CALL doughnut_repair_an_example_of_tracker_keys();
DROP PROCEDURE IF EXISTS doughnut_repair_an_example_of_tracker_keys;

DELETE FROM note_property_index
WHERE LOWER(TRIM(property_key)) = 'an example of'
   OR LOWER(TRIM(property_key)) REGEXP '^an example of [0-9]+$';

DROP PROCEDURE IF EXISTS doughnut_repair_example_of_property_index;
DELIMITER $$
CREATE PROCEDURE doughnut_repair_example_of_property_index()
BEGIN
  DECLARE done INT DEFAULT FALSE;
  DECLARE v_note_id INT UNSIGNED;
  DECLARE v_content MEDIUMTEXT;
  DECLARE v_norm MEDIUMTEXT;
  DECLARE v_after_open MEDIUMTEXT;
  DECLARE v_close_pos INT;
  DECLARE v_frontmatter MEDIUMTEXT;
  DECLARE v_slot INT;
  DECLARE v_candidate VARCHAR(255);
  DECLARE v_key_found INT;

  DECLARE cur CURSOR FOR
    SELECT id, content
    FROM note
    WHERE deleted_at IS NULL
      AND content LIKE '%example of%'
    ORDER BY id ASC;

  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

  OPEN cur;
  read_loop: LOOP
    FETCH cur INTO v_note_id, v_content;
    IF done THEN
      LEAVE read_loop;
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
      ITERATE read_loop;
    END IF;

    SET v_after_open = SUBSTRING(v_norm, 5);
    SET v_close_pos = LOCATE(CONCAT(CHAR(10), '---', CHAR(10)), v_after_open);
    IF v_close_pos = 0 THEN
      SET v_close_pos = LOCATE(CONCAT(CHAR(10), '---'), v_after_open);
      IF v_close_pos = 0
        OR NOT (
          v_close_pos + 4 = LENGTH(v_after_open)
          OR SUBSTRING(v_after_open, v_close_pos + 4) = ''
        ) THEN
        ITERATE read_loop;
      END IF;
    END IF;

    SET v_frontmatter = SUBSTRING(v_after_open, 1, v_close_pos - 1);
    SET v_slot = 1;

    slot_loop: WHILE v_slot <= 100 DO
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
        OR LEFT(LTRIM(LOWER(v_frontmatter)), LENGTH(v_candidate) + 1) = CONCAT(LOWER(v_candidate), ':')
        OR LOCATE(
          CONCAT(CHAR(10), CONCAT('"', LOWER(v_candidate), '"'), ':'),
          CONCAT(CHAR(10), LOWER(v_frontmatter), CHAR(10))
        ) > 0
      );

      IF v_key_found THEN
        INSERT IGNORE INTO note_property_index (note_id, property_key)
        VALUES (v_note_id, (v_candidate COLLATE utf8mb4_0900_ai_ci));
      END IF;

      SET v_slot = v_slot + 1;
    END WHILE slot_loop;
  END LOOP;
  CLOSE cur;
END$$
DELIMITER ;

CALL doughnut_repair_example_of_property_index();
DROP PROCEDURE IF EXISTS doughnut_repair_example_of_property_index;

INSERT INTO memory_tracker (
  user_id,
  note_id,
  spelling,
  property_key,
  removed_from_tracking,
  assimilated_at,
  last_recalled_at,
  next_recall_at,
  forgetting_curve_index,
  recall_count
)
SELECT
  o.user_id,
  npi.note_id,
  0,
  npi.property_key,
  1,
  UTC_TIMESTAMP(),
  UTC_TIMESTAMP(),
  UTC_TIMESTAMP(),
  100.0,
  0
FROM note_property_index npi
INNER JOIN note n ON n.id = npi.note_id AND n.deleted_at IS NULL
INNER JOIN notebook nb ON nb.id = n.notebook_id AND nb.deleted_at IS NULL
INNER JOIN ownership o ON o.id = nb.ownership_id AND o.user_id IS NOT NULL
WHERE LOWER(TRIM(npi.property_key)) <> 'example of'
  AND NOT (
    LOWER(TRIM(npi.property_key)) LIKE 'example of %'
    AND CAST(TRIM(SUBSTRING(LOWER(TRIM(npi.property_key)), 12)) AS UNSIGNED) >= 2
  )
  AND LOWER(TRIM(npi.property_key)) NOT IN (
    'image',
    'image_mask',
    'imagemask',
    'wikidata_id',
    'wikidataid',
    'url',
    'title_pattern',
    'titlepattern',
    'question_generation_instruction',
    'questiongenerationinstruction',
    'type',
    'relation',
    'source',
    'target'
  )
  AND NOT EXISTS (
    SELECT 1
    FROM memory_tracker mt
    WHERE mt.user_id = o.user_id
      AND mt.note_id = npi.note_id
      AND mt.spelling = 0
      AND mt.property_key = npi.property_key
      AND mt.deleted_at IS NULL
  );
