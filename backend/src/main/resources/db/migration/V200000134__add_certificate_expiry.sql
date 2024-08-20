ALTER TABLE notebook
ADD COLUMN certificate_expiry VARCHAR(64) DEFAULT "P1Y";
