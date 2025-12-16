-- Remove unused fields from note table
ALTER TABLE note DROP COLUMN use_parent_picture;
ALTER TABLE note DROP COLUMN picture_mask;
ALTER TABLE note DROP COLUMN construct_type_id;

-- Rename title_constructor to title
ALTER TABLE note CHANGE COLUMN title_constructor title varchar(150) DEFAULT NULL;

