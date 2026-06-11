-- V300000202 moved "an-example-of" relationship notes into source-note frontmatter
-- properties, but some production rows missed the matching wiki-link cache insert.
-- Re-scan notes whose leading frontmatter mentions "an example of" and insert any
-- missing cache rows for resolvable wiki links. This is additive only: existing
-- cache rows are preserved.

DROP PROCEDURE IF EXISTS doughnut_repair_an_example_of_wiki_link_cache;
DELIMITER $$
CREATE PROCEDURE doughnut_repair_an_example_of_wiki_link_cache()
BEGIN
  DECLARE done INT DEFAULT FALSE;
  DECLARE v_note_id INT UNSIGNED;
  DECLARE v_notebook_id INT UNSIGNED;
  DECLARE v_content MEDIUMTEXT;
  DECLARE v_norm MEDIUMTEXT;
  DECLARE v_frontmatter MEDIUMTEXT;
  DECLARE v_close_pos INT;
  DECLARE v_scan_pos INT;
  DECLARE v_link_start INT;
  DECLARE v_link_end INT;
  DECLARE v_link_text VARCHAR(767);
  DECLARE v_target_token VARCHAR(767);
  DECLARE v_pipe_pos INT;
  DECLARE v_colon_pos INT;
  DECLARE v_target_notebook_name VARCHAR(150);
  DECLARE v_target_title VARCHAR(150);
  DECLARE v_target_note_id INT UNSIGNED;

  DECLARE cur CURSOR FOR
    SELECT id, notebook_id, content
    FROM note
    WHERE deleted_at IS NULL
      AND content LIKE '%an example of%'
    ORDER BY id ASC;

  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

  OPEN cur;
  read_loop: LOOP
    FETCH cur INTO v_note_id, v_notebook_id, v_content;
    IF done THEN
      LEAVE read_loop;
    END IF;

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

    SET v_close_pos = LOCATE(CONCAT(CHAR(10), '---', CHAR(10)), SUBSTRING(v_norm, 5));
    IF v_close_pos = 0 THEN
      ITERATE read_loop;
    END IF;

    SET v_frontmatter = SUBSTRING(SUBSTRING(v_norm, 5), 1, v_close_pos - 1);
    IF LOWER(v_frontmatter) NOT LIKE '%an example of%' THEN
      ITERATE read_loop;
    END IF;

    SET v_scan_pos = 1;
    link_loop: LOOP
      SET v_link_start = LOCATE('[[', v_norm, v_scan_pos);
      IF v_link_start = 0 THEN
        LEAVE link_loop;
      END IF;

      SET v_link_end = LOCATE(']]', v_norm, v_link_start + 2);
      IF v_link_end = 0 THEN
        LEAVE link_loop;
      END IF;

      IF v_link_end - v_link_start - 2 <= 767 THEN
        SET v_link_text = TRIM(SUBSTRING(v_norm, v_link_start + 2, v_link_end - v_link_start - 2));
      ELSE
        SET v_link_text = '';
      END IF;
      SET v_scan_pos = v_link_end + 2;

      IF v_link_text = '' THEN
        ITERATE link_loop;
      END IF;

      SET v_pipe_pos = LOCATE('|', v_link_text);
      IF v_pipe_pos > 0 THEN
        SET v_target_token = TRIM(SUBSTRING(v_link_text, 1, v_pipe_pos - 1));
      ELSE
        SET v_target_token = TRIM(v_link_text);
      END IF;

      IF v_target_token = '' THEN
        ITERATE link_loop;
      END IF;

      SET v_colon_pos = LOCATE(':', v_target_token);
      SET v_target_note_id = NULL;

      IF v_colon_pos > 1 AND v_colon_pos < CHAR_LENGTH(v_target_token) THEN
        SET v_target_notebook_name = TRIM(SUBSTRING(v_target_token, 1, v_colon_pos - 1));
        SET v_target_title = TRIM(SUBSTRING(v_target_token, v_colon_pos + 1));

        SET v_target_note_id = (
          SELECT t.id
          FROM note t
          INNER JOIN notebook nb ON nb.id = t.notebook_id
          WHERE t.deleted_at IS NULL
            AND nb.deleted_at IS NULL
            AND LOWER(nb.name) = LOWER(v_target_notebook_name)
            AND LOWER(t.title) = LOWER(v_target_title)
          ORDER BY t.id ASC
          LIMIT 1
        );
      ELSEIF v_notebook_id IS NOT NULL THEN
        SET v_target_title = v_target_token;

        SET v_target_note_id = (
          SELECT t.id
          FROM note t
          WHERE t.deleted_at IS NULL
            AND t.notebook_id = v_notebook_id
            AND LOWER(t.title) = LOWER(v_target_title)
          ORDER BY t.id ASC
          LIMIT 1
        );
      END IF;

      IF v_target_note_id IS NOT NULL
        AND NOT EXISTS (
          SELECT 1
          FROM note_wiki_title_cache c
          WHERE c.note_id = v_note_id
            AND LOWER(c.link_text) = LOWER(v_link_text)
        ) THEN
        INSERT IGNORE INTO note_wiki_title_cache (note_id, target_note_id, link_text)
        VALUES (
          v_note_id,
          v_target_note_id,
          (v_link_text COLLATE utf8mb4_bin)
        );
      END IF;
    END LOOP;
  END LOOP;
  CLOSE cur;
END$$
DELIMITER ;

CALL doughnut_repair_an_example_of_wiki_link_cache();
DROP PROCEDURE IF EXISTS doughnut_repair_an_example_of_wiki_link_cache;
