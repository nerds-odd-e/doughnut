ALTER TABLE notebook
  ADD COLUMN name VARCHAR(150) NULL;

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
SET hn.title = 'index',
    hn.slug = 'index',
    hn.folder_id = NULL,
    hn.parent_id = NULL
WHERE hn.deleted_at IS NULL;
