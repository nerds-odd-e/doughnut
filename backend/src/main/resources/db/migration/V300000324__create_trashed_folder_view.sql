CREATE VIEW trashed_folder AS
WITH RECURSIVE members AS (
  SELECT id
  FROM folder
  WHERE parent_folder_id IS NULL
    AND LOWER(name) = '_trash'

  UNION ALL

  SELECT child.id
  FROM folder child
  JOIN members parent ON child.parent_folder_id = parent.id
)
SELECT id
FROM members;
