ALTER TABLE note
ADD COLUMN parent_id INT UNSIGNED NULL,
ADD FOREIGN KEY (parent_id) REFERENCES note (id) ON DELETE SET NULL;

UPDATE note n
INNER JOIN notes_closure nc ON n.id = nc.note_id AND nc.depth = 1
SET n.parent_id = nc.ancestor_id;

