-- Halfwidth / in cached wiki link text → fullwidth ／ (U+FF0F), mirroring note title migration.
-- Uses note_wiki_title_cache to find affected links; updates referrer note.content (body + frontmatter)
-- and cache link_text. _utf8mb4 literals for Cloud SQL compatibility.

DROP PROCEDURE IF EXISTS doughnut_fix_wiki_link_text_slashes;
DELIMITER $$
CREATE PROCEDURE doughnut_fix_wiki_link_text_slashes()
BEGIN
  DECLARE done INT DEFAULT FALSE;
  DECLARE v_note_id INT UNSIGNED;
  DECLARE v_old_text VARCHAR(767);
  DECLARE v_new_text VARCHAR(767);

  DECLARE cur CURSOR FOR
    SELECT DISTINCT c.note_id, c.link_text
    FROM note_wiki_title_cache c
    WHERE LOCATE(CHAR(47), c.link_text) > 0
    ORDER BY CHAR_LENGTH(c.link_text) DESC, c.note_id, c.link_text;

  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

  OPEN cur;
  read_loop: LOOP
    FETCH cur INTO v_note_id, v_old_text;
    IF done THEN
      LEAVE read_loop;
    END IF;

    SET v_new_text = REPLACE(v_old_text, CHAR(47), _utf8mb4'／');

    UPDATE note
    SET content = REPLACE(content, CONCAT('[[', v_old_text, ']]'), CONCAT('[[', v_new_text, ']]'))
    WHERE id = v_note_id
      AND LOCATE(CONCAT('[[', v_old_text, ']]'), COALESCE(content, '')) > 0;
  END LOOP;
  CLOSE cur;
END$$
DELIMITER ;

CALL doughnut_fix_wiki_link_text_slashes();
DROP PROCEDURE IF EXISTS doughnut_fix_wiki_link_text_slashes;

-- Drop slash-spelling cache rows when the fullwidth spelling already exists for the same note.
DELETE old_row
FROM note_wiki_title_cache old_row
INNER JOIN note_wiki_title_cache existing_row
  ON existing_row.note_id = old_row.note_id
  AND existing_row.link_text = REPLACE(old_row.link_text, CHAR(47), _utf8mb4'／')
WHERE LOCATE(CHAR(47), old_row.link_text) > 0
  AND old_row.id <> existing_row.id;

UPDATE note_wiki_title_cache
SET link_text = REPLACE(link_text, CHAR(47), _utf8mb4'／')
WHERE LOCATE(CHAR(47), link_text) > 0;
