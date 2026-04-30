UPDATE note n
INNER JOIN note p ON p.id = n.parent_id
INNER JOIN note t ON t.id = n.target_note_id
SET n.title = LEFT(
  TRIM(CONCAT_WS(
    ' ',
    CASE WHEN TRIM(COALESCE(p.title, '')) = '' THEN 'Untitled' ELSE TRIM(COALESCE(p.title, '')) END,
    CASE
      WHEN TRIM(COALESCE(n.relation_type, '')) = '' THEN 'related to'
      ELSE TRIM(COALESCE(n.relation_type, ''))
    END,
    CASE WHEN TRIM(COALESCE(t.title, '')) = '' THEN 'Untitled' ELSE TRIM(COALESCE(t.title, '')) END
  )),
  150
)
WHERE n.target_note_id IS NOT NULL
  AND n.relation_type IS NOT NULL
  AND n.parent_id IS NOT NULL
  AND (n.title IS NULL OR TRIM(n.title) = '');

UPDATE note
SET title = 'Untitled'
WHERE title IS NULL OR TRIM(title) = '';

ALTER TABLE note MODIFY COLUMN title VARCHAR(150) NOT NULL;
