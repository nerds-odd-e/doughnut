ALTER TABLE note
ADD COLUMN use_parent_picture BOOLEAN;
ALTER TABLE note
ADD COLUMN picture_mask VARCHAR(1024);

UPDATE note SET use_parent_picture = 0;

ALTER TABLE note
    MODIFY use_parent_picture BOOLEAN DEFAULT FALSE NOT NULL;
