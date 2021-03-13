ALTER TABLE link
    ADD COLUMN type VARCHAR(20);

UPDATE link SET type = "belongs to";

ALTER TABLE link
    MODIFY type VARCHAR(20) NOT NULL;