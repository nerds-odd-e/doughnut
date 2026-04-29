SET @__m154_add_notebook_name := (
  SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notebook' AND COLUMN_NAME = 'name') = 0,
    'ALTER TABLE notebook ADD COLUMN name VARCHAR(150) NULL',
    'DO 0'));
PREPARE __m154_stmt FROM @__m154_add_notebook_name;
EXECUTE __m154_stmt;
DEALLOCATE PREPARE __m154_stmt;

UPDATE notebook n
INNER JOIN notebook_head_note nh ON nh.notebook_id = n.id
INNER JOIN note hn ON nh.head_note_id = hn.id AND hn.deleted_at IS NULL
SET n.name = LEFT(hn.title, 150);

UPDATE note n
INNER JOIN notebook_head_note nh ON nh.notebook_id = n.notebook_id
SET n.slug = CONCAT('legacy-index-', n.id)
WHERE n.id <> nh.head_note_id
  AND n.slug = 'index'
  AND n.deleted_at IS NULL;

UPDATE note hn
INNER JOIN notebook_head_note nh ON nh.head_note_id = hn.id
SET hn.notebook_id = nh.notebook_id,
    hn.title = 'index',
    hn.slug = 'index',
    hn.folder_id = NULL,
    hn.parent_id = NULL
WHERE hn.deleted_at IS NULL;
