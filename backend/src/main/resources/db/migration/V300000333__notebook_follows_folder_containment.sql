-- Legacy rows hold folder ancestry that crosses notebooks. Containment wins: every folder takes
-- its root ancestor's notebook and every note takes its folder's. No-op where no row is stray.
WITH RECURSIVE rooted AS (
  SELECT id, notebook_id AS root_notebook_id FROM folder WHERE parent_folder_id IS NULL
  UNION ALL
  SELECT child.id, rooted.root_notebook_id
  FROM folder child JOIN rooted ON child.parent_folder_id = rooted.id
)
UPDATE folder JOIN rooted ON rooted.id = folder.id
SET folder.notebook_id = rooted.root_notebook_id, folder.updated_at = folder.updated_at
WHERE folder.notebook_id <> rooted.root_notebook_id;

UPDATE note JOIN folder ON folder.id = note.folder_id
SET note.notebook_id = folder.notebook_id, note.updated_at = note.updated_at
WHERE note.notebook_id <> folder.notebook_id;
