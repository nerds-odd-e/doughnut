DROP TABLE certificate;

ALTER TABLE notebook
DROP COLUMN until_cert_expire,
DROP COLUMN certified_by;
