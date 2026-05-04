-- Unique note title per (notebook, folder placement): rename duplicates, then enforce with a functional unique key.
-- Folder placement: IFNULL(folder_id, 0) matches top-level notes (NULL folder) to one bucket, same pattern as uk_folder_notebook_parent_name.
-- Notebook scope: IFNULL(notebook_id, 0) so NULL notebook_id rows are included.
-- Pass 1: within each duplicate group on (notebook, folder, title), keep lowest id; assign title-1, title-2, ... (truncated to varchar(150)).
-- Pass 2: while any duplicates remain (e.g. renamed row collides with an existing title), rename the highest-id row in each conflict group to base-'id' (truncated).

UPDATE note n
INNER JOIN (
  SELECT
    id,
    CASE
      WHEN rn = 1 THEN title
      ELSE CONCAT(
        LEFT(
          title,
          GREATEST(
            1,
            150 - CHAR_LENGTH(CONCAT('-', rn - 1))
          )
        ),
        '-',
        (rn - 1)
      )
    END AS new_title
  FROM (
    SELECT
      id,
      title,
      ROW_NUMBER() OVER (
        PARTITION BY IFNULL(notebook_id, 0), IFNULL(folder_id, 0), title
        ORDER BY id
      ) AS rn
    FROM note
  ) ranked
) u ON n.id = u.id
SET n.title = u.new_title;

DROP PROCEDURE IF EXISTS doughnut_fix_note_title_dupes;

DELIMITER $$

CREATE PROCEDURE doughnut_fix_note_title_dupes()
BEGIN
  DECLARE v_dups INT DEFAULT 1;
  DECLARE v_i INT DEFAULT 0;
  WHILE v_dups > 0 AND v_i < 1000000 DO
    UPDATE note n
    INNER JOIN (
      SELECT MAX(id) AS max_id
      FROM note
      GROUP BY IFNULL(notebook_id, 0), IFNULL(folder_id, 0), title
      HAVING COUNT(*) > 1
    ) d ON n.id = d.max_id
    SET n.title = CONCAT(
      LEFT(n.title, GREATEST(1, 150 - CHAR_LENGTH(CAST(n.id AS CHAR)) - 1)),
      '-',
      n.id
    );
    SELECT COUNT(*) INTO v_dups FROM (
      SELECT 1 AS x
      FROM note
      GROUP BY IFNULL(notebook_id, 0), IFNULL(folder_id, 0), title
      HAVING COUNT(*) > 1
    ) t;
    SET v_i = v_i + 1;
  END WHILE;
END$$

DELIMITER ;

CALL doughnut_fix_note_title_dupes();

DROP PROCEDURE IF EXISTS doughnut_fix_note_title_dupes;

ALTER TABLE note
  ADD UNIQUE KEY uk_note_notebook_folder_title (
    (ifnull(`notebook_id`, 0)),
    (ifnull(`folder_id`, 0)),
    `title`
  );
