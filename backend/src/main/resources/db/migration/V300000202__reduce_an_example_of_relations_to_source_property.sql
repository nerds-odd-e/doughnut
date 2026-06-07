-- Reduces every non-deleted relationship note with relation: an-example-of into a property
-- on its resolved source note (key family: "an example of", "an example of 2", …).
-- Re-homes active note-level memory trackers; clears recall_prompt rows that
-- reference the relation note's predefined_question rows (no ON DELETE CASCADE);
-- hard-deletes the relation note.
-- Then drops all empty folders (no non-deleted notes, no child folders), preserving
-- folders with non-blank index_content.

DROP PROCEDURE IF EXISTS doughnut_reduce_an_example_of_relations;
DELIMITER $$
CREATE PROCEDURE doughnut_reduce_an_example_of_relations()
BEGIN
  DECLARE done INT DEFAULT FALSE;
  DECLARE v_relation_note_id INT UNSIGNED;
  DECLARE v_content MEDIUMTEXT;
  DECLARE v_raw MEDIUMTEXT;
  DECLARE v_norm MEDIUMTEXT;
  DECLARE v_padded MEDIUMTEXT;
  DECLARE v_frontmatter MEDIUMTEXT;
  DECLARE v_source_fm MEDIUMTEXT;
  DECLARE v_source_raw MEDIUMTEXT;
  DECLARE v_source_norm MEDIUMTEXT;
  DECLARE v_source_padded MEDIUMTEXT;
  DECLARE v_type_part VARCHAR(255);
  DECLARE v_relation_part VARCHAR(255);
  DECLARE v_source_part VARCHAR(1024);
  DECLARE v_target_part VARCHAR(1024);
  DECLARE v_source_inner VARCHAR(767);
  DECLARE v_target_link VARCHAR(1024);
  DECLARE v_target_inner VARCHAR(767);
  DECLARE v_source_note_id INT UNSIGNED;
  DECLARE v_target_note_id INT UNSIGNED;
  DECLARE v_resolved_key VARCHAR(255);
  DECLARE v_candidate VARCHAR(255);
  DECLARE v_yaml_addition MEDIUMTEXT;
  DECLARE v_new_content MEDIUMTEXT;
  DECLARE v_close_pos INT;
  DECLARE v_slot INT;
  DECLARE v_key_found INT;
  DECLARE v_source_inner_start INT;
  DECLARE v_source_inner_end INT;
  DECLARE v_target_link_start INT;
  DECLARE v_target_link_end INT;
  DECLARE v_type_pos INT;
  DECLARE v_relation_pos INT;
  DECLARE v_source_pos INT;
  DECLARE v_target_pos INT;
  DECLARE v_fm_inner_pos INT;

  DECLARE cur CURSOR FOR
    SELECT id, content
    FROM note
    WHERE deleted_at IS NULL
      AND content LIKE '%an-example-of%'
    ORDER BY id ASC;

  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

  OPEN cur;
  read_loop: LOOP
    FETCH cur INTO v_relation_note_id, v_content;
    IF done THEN
      LEAVE read_loop;
    END IF;
    SET done = FALSE;

    SET v_raw = COALESCE(v_content, '');
    SET v_norm = REPLACE(
      REPLACE(
        IF(
          LENGTH(v_raw) >= 3 AND HEX(LEFT(v_raw, 1)) = 'EFBBBF',
          SUBSTRING(v_raw, 2),
          v_raw
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

    SET v_close_pos = LOCATE(CONCAT(CHAR(10), '---', CHAR(10)), SUBSTRING(v_norm, 5));
    IF v_close_pos = 0 THEN
      ITERATE read_loop;
    END IF;

    SET v_frontmatter = SUBSTRING(SUBSTRING(v_norm, 5), 1, v_close_pos - 1);

    SET v_type_pos = LOCATE('type:', LOWER(v_frontmatter));
    IF v_type_pos = 0 THEN
      ITERATE read_loop;
    END IF;
    SET v_type_part = TRIM(
      BOTH '"' FROM TRIM(
        BOTH '''' FROM TRIM(
          SUBSTRING_INDEX(
            SUBSTRING(v_frontmatter, v_type_pos + 5),
            CHAR(10),
            1
          )
        )
      )
    );
    IF LOWER(v_type_part) <> 'relationship' THEN
      ITERATE read_loop;
    END IF;

    SET v_relation_pos = LOCATE('relation:', LOWER(v_frontmatter));
    IF v_relation_pos = 0 THEN
      ITERATE read_loop;
    END IF;
    SET v_relation_part = TRIM(
      BOTH '"' FROM TRIM(
        BOTH '''' FROM TRIM(
          SUBSTRING_INDEX(
            SUBSTRING(v_frontmatter, v_relation_pos + 9),
            CHAR(10),
            1
          )
        )
      )
    );
    IF v_relation_part <> 'an-example-of' THEN
      ITERATE read_loop;
    END IF;

    SET v_source_pos = LOCATE('source:', LOWER(v_frontmatter));
    IF v_source_pos = 0 THEN
      ITERATE read_loop;
    END IF;
    SET v_source_part = TRIM(
      BOTH '"' FROM TRIM(
        BOTH '''' FROM TRIM(
          SUBSTRING_INDEX(
            SUBSTRING(v_frontmatter, v_source_pos + 7),
            CHAR(10),
            1
          )
        )
      )
    );
    SET v_source_inner_start = LOCATE('[[', v_source_part);
    SET v_source_inner_end = LOCATE(']]', v_source_part);
    IF v_source_inner_start = 0 OR v_source_inner_end = 0 OR v_source_inner_end <= v_source_inner_start THEN
      ITERATE read_loop;
    END IF;
    SET v_source_inner = TRIM(
      SUBSTRING(
        v_source_part,
        v_source_inner_start + 2,
        v_source_inner_end - v_source_inner_start - 2
      )
    );
    IF v_source_inner = '' THEN
      ITERATE read_loop;
    END IF;

    SET v_target_pos = LOCATE('target:', LOWER(v_frontmatter));
    IF v_target_pos = 0 THEN
      ITERATE read_loop;
    END IF;
    SET v_target_part = TRIM(
      SUBSTRING_INDEX(
        SUBSTRING(v_frontmatter, v_target_pos + 7),
        CHAR(10),
        1
      )
    );
    SET v_target_link_start = LOCATE('[[', v_target_part);
    SET v_target_link_end = LOCATE(']]', v_target_part);
    IF v_target_link_start = 0 OR v_target_link_end = 0 OR v_target_link_end <= v_target_link_start THEN
      ITERATE read_loop;
    END IF;
    SET v_target_link = SUBSTRING(
      v_target_part,
      v_target_link_start,
      v_target_link_end - v_target_link_start + 2
    );
    SET v_target_inner = TRIM(
      SUBSTRING(v_target_link, 3, LENGTH(v_target_link) - 4)
    );
    IF v_target_inner = '' THEN
      ITERATE read_loop;
    END IF;

    SET v_source_note_id = (
      SELECT c.target_note_id
      FROM note_wiki_title_cache c
      INNER JOIN note sn ON sn.id = c.target_note_id AND sn.deleted_at IS NULL
      WHERE c.note_id = v_relation_note_id
        AND c.link_text = (v_source_inner COLLATE utf8mb4_bin)
      LIMIT 1
    );

    IF v_source_note_id IS NULL THEN
      ITERATE read_loop;
    END IF;

    SET v_source_raw = (
      SELECT content
      FROM note
      WHERE id = v_source_note_id
    );

    SET v_source_norm = REPLACE(
      REPLACE(
        IF(
          LENGTH(COALESCE(v_source_raw, '')) >= 3 AND HEX(LEFT(COALESCE(v_source_raw, ''), 1)) = 'EFBBBF',
          SUBSTRING(COALESCE(v_source_raw, ''), 2),
          COALESCE(v_source_raw, '')
        ),
        CONCAT(CHAR(13), CHAR(10)),
        CHAR(10)
      ),
      CHAR(13),
      CHAR(10)
    );

    IF SUBSTRING_INDEX(v_source_norm, CHAR(10), 1) = '---' THEN
      SET v_fm_inner_pos = LOCATE(CONCAT(CHAR(10), '---', CHAR(10)), SUBSTRING(v_source_norm, 5));
      IF v_fm_inner_pos > 0 THEN
        SET v_source_fm = SUBSTRING(SUBSTRING(v_source_norm, 5), 1, v_fm_inner_pos - 1);
      ELSE
        SET v_source_fm = '';
      END IF;
    ELSE
      SET v_source_fm = '';
    END IF;

    SET v_slot = 1;
    find_key: LOOP
      IF v_slot = 1 THEN
        SET v_candidate = 'an example of';
      ELSE
        SET v_candidate = CONCAT('an example of ', v_slot);
      END IF;

      SET v_key_found = (
        LOCATE(
          CONCAT(CHAR(10), LOWER(v_candidate), ':'),
          CONCAT(CHAR(10), LOWER(v_source_fm), CHAR(10))
        ) > 0
        OR LEFT(LTRIM(v_source_fm), LENGTH(v_candidate) + 1) = CONCAT(v_candidate, ':')
        OR LEFT(LTRIM(LOWER(v_source_fm)), LENGTH(v_candidate) + 1) = CONCAT(LOWER(v_candidate), ':')
      );

      IF v_key_found THEN
        IF v_slot = 1 THEN
          SET v_slot = 2;
        ELSE
          SET v_slot = v_slot + 1;
        END IF;
        IF v_slot > 100 THEN
          ITERATE read_loop;
        END IF;
      ELSE
        SET v_resolved_key = v_candidate;
        LEAVE find_key;
      END IF;
    END LOOP find_key;

    SET v_yaml_addition = CONCAT(v_resolved_key, ': "', v_target_link, '"', CHAR(10));

    SET v_source_padded = IF(
      LENGTH(v_source_norm) > 0 AND RIGHT(v_source_norm, 1) = CHAR(10),
      v_source_norm,
      CONCAT(v_source_norm, CHAR(10))
    );

    SET v_new_content = CASE
      WHEN SUBSTRING_INDEX(v_source_norm, CHAR(10), 1) <> '---'
      THEN CONCAT('---', CHAR(10), v_yaml_addition, '---', CHAR(10), CHAR(10), COALESCE(v_source_raw, ''))
      WHEN LEFT(SUBSTRING(v_source_padded, 5), 4) = CONCAT('---', CHAR(10))
      THEN CONCAT(
        '---', CHAR(10),
        v_yaml_addition,
        '---', CHAR(10),
        SUBSTRING(SUBSTRING(v_source_norm, 5), 5)
      )
      WHEN LOCATE(CONCAT(CHAR(10), '---', CHAR(10)), SUBSTRING(v_source_padded, 5)) > 0
      THEN CONCAT(
        '---', CHAR(10),
        v_yaml_addition,
        SUBSTRING(
          SUBSTRING(v_source_norm, 5),
          1,
          LOCATE(CONCAT(CHAR(10), '---', CHAR(10)), SUBSTRING(v_source_padded, 5)) - 1
        ),
        CHAR(10), '---', CHAR(10),
        SUBSTRING(
          SUBSTRING(v_source_norm, 5),
          LOCATE(CONCAT(CHAR(10), '---', CHAR(10)), SUBSTRING(v_source_padded, 5)) + 5
        )
      )
      ELSE CONCAT('---', CHAR(10), v_yaml_addition, '---', CHAR(10), CHAR(10), v_source_norm)
    END;

    UPDATE note
    SET content = v_new_content,
        updated_at = UTC_TIMESTAMP()
    WHERE id = v_source_note_id;

    SET v_target_note_id = (
      SELECT c.target_note_id
      FROM note_wiki_title_cache c
      WHERE c.note_id = v_relation_note_id
        AND c.link_text = (v_target_inner COLLATE utf8mb4_bin)
      LIMIT 1
    );

    IF v_target_note_id IS NOT NULL THEN
      INSERT IGNORE INTO note_wiki_title_cache (note_id, target_note_id, link_text)
      VALUES (
        v_source_note_id,
        v_target_note_id,
        (v_target_inner COLLATE utf8mb4_bin)
      );
    END IF;

    UPDATE memory_tracker mt
    SET mt.note_id = v_source_note_id,
        mt.property_key = (v_resolved_key COLLATE utf8mb4_0900_ai_ci)
    WHERE mt.note_id = v_relation_note_id
      AND mt.deleted_at IS NULL
      AND mt.removed_from_tracking = 0
      AND mt.spelling = 0
      AND mt.property_key = ''
      AND NOT EXISTS (
        SELECT 1
        FROM (
          SELECT user_id, note_id, spelling, property_key, deleted_at, removed_from_tracking
          FROM memory_tracker
        ) existing
        WHERE existing.user_id = mt.user_id
          AND existing.note_id = v_source_note_id
          AND existing.spelling = 0
          AND existing.property_key = (v_resolved_key COLLATE utf8mb4_0900_ai_ci)
          AND existing.deleted_at IS NULL
          AND existing.removed_from_tracking = 0
      );

    DELETE rp
    FROM recall_prompt rp
    INNER JOIN predefined_question pq ON pq.id = rp.predefined_question_id
    WHERE pq.note_id = v_relation_note_id;

    DELETE FROM note WHERE id = v_relation_note_id;
  END LOOP;
  CLOSE cur;
END$$
DELIMITER ;

CALL doughnut_reduce_an_example_of_relations();
DROP PROCEDURE IF EXISTS doughnut_reduce_an_example_of_relations;

DROP PROCEDURE IF EXISTS doughnut_drop_empty_folders;
DELIMITER $$
CREATE PROCEDURE doughnut_drop_empty_folders()
BEGIN
  DECLARE v_rows INT DEFAULT 1;

  REPEAT
    DELETE FROM folder
    WHERE id IN (
      SELECT empty_id
      FROM (
        SELECT f.id AS empty_id
        FROM folder f
        WHERE NOT EXISTS (
            SELECT 1
            FROM note n
            WHERE n.folder_id = f.id
              AND n.deleted_at IS NULL
          )
          AND NOT EXISTS (
            SELECT 1
            FROM folder c
            WHERE c.parent_folder_id = f.id
          )
          AND (f.index_content IS NULL OR TRIM(f.index_content) = '')
      ) empty_folders
    );
    SET v_rows = ROW_COUNT();
  UNTIL v_rows = 0 END REPEAT;
END$$
DELIMITER ;

CALL doughnut_drop_empty_folders();
DROP PROCEDURE IF EXISTS doughnut_drop_empty_folders;
