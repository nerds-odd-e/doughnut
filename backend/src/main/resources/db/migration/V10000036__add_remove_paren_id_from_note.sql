ALTER TABLE note
DROP FOREIGN KEY fk_parent_id,
DROP COLUMN parent_id;

ALTER TABLE note
RENAME COLUMN picture to picture_url;

