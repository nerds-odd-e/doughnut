ALTER TABLE circle
ADD COLUMN invitation_code VARCHAR(20);
ALTER TABLE circle
    ADD UNIQUE INDEX (invitation_code);
