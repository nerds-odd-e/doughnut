ALTER TABLE notebook
    ADD COLUMN notebook_type VARCHAR(64);

UPDATE notebook SET notebook_type = 'GENERAL';