-- Enforce note title uniqueness case-insensitively without changing byte-accurate title storage.
-- LOWER(title) folds case for scripts with case, while the utf8mb4_bin title collation keeps
-- kana, accents, and other non-case distinctions intact.

DROP PROCEDURE IF EXISTS doughnut_fix_note_title_case_dupes;
DELIMITER $$
CREATE PROCEDURE doughnut_fix_note_title_case_dupes()
BEGIN
  DECLARE done INT DEFAULT FALSE;
  DECLARE v_note_id INT UNSIGNED;
  DECLARE v_notebook_id INT UNSIGNED;
  DECLARE v_folder_id INT UNSIGNED;
  DECLARE v_original_title VARCHAR(150);
  DECLARE v_suffix INT;
  DECLARE v_suffix_text VARCHAR(20);
  DECLARE v_candidate VARCHAR(150);

  DECLARE cur CURSOR FOR
    SELECT id, notebook_id, folder_id, title, rn
    FROM (
      SELECT
        id,
        notebook_id,
        folder_id,
        title,
        ROW_NUMBER() OVER (
          PARTITION BY IFNULL(notebook_id, 0), IFNULL(folder_id, 0), LOWER(title)
          ORDER BY id
        ) AS rn
      FROM note
    ) ranked
    WHERE rn > 1
    ORDER BY id;

  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

  OPEN cur;
  read_loop: LOOP
    FETCH cur INTO v_note_id, v_notebook_id, v_folder_id, v_original_title, v_suffix;
    IF done THEN
      LEAVE read_loop;
    END IF;

    candidate_loop: LOOP
      SET v_suffix_text = CONCAT(' (', v_suffix, ')');
      SET v_candidate = CONCAT(
        LEFT(v_original_title, GREATEST(1, 150 - CHAR_LENGTH(v_suffix_text))),
        v_suffix_text
      );

      IF NOT EXISTS (
        SELECT 1
        FROM note
        WHERE id <> v_note_id
          AND IFNULL(notebook_id, 0) = IFNULL(v_notebook_id, 0)
          AND IFNULL(folder_id, 0) = IFNULL(v_folder_id, 0)
          AND LOWER(title) = LOWER(v_candidate)
      ) THEN
        UPDATE note
        SET title = v_candidate
        WHERE id = v_note_id;
        LEAVE candidate_loop;
      END IF;

      SET v_suffix = v_suffix + 1;
    END LOOP;
  END LOOP;
  CLOSE cur;
END$$
DELIMITER ;

CALL doughnut_fix_note_title_case_dupes();
DROP PROCEDURE IF EXISTS doughnut_fix_note_title_case_dupes;

DROP PROCEDURE IF EXISTS doughnut_assert_no_note_title_case_dupes;
DELIMITER $$
CREATE PROCEDURE doughnut_assert_no_note_title_case_dupes()
BEGIN
  DECLARE v_case_dupes INT DEFAULT 0;

  SELECT COUNT(*) INTO v_case_dupes
  FROM (
    SELECT 1
    FROM note
    GROUP BY IFNULL(notebook_id, 0), IFNULL(folder_id, 0), LOWER(title)
    HAVING COUNT(*) > 1
  ) duplicate_groups;

  IF v_case_dupes > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Case-insensitive note title duplicates remain after auto-rename';
  END IF;
END$$
DELIMITER ;

CALL doughnut_assert_no_note_title_case_dupes();
DROP PROCEDURE IF EXISTS doughnut_assert_no_note_title_case_dupes;

ALTER TABLE `note`
  DROP INDEX `uk_note_notebook_folder_title`,
  DROP INDEX `idx_note_notebook_id_title`,
  ADD UNIQUE KEY `uk_note_notebook_folder_title` (
    (ifnull(`notebook_id`, 0)),
    (ifnull(`folder_id`, 0)),
    (lower(`title`))
  ),
  ADD KEY `idx_note_notebook_id_title` (`notebook_id`, (lower(`title`)));
