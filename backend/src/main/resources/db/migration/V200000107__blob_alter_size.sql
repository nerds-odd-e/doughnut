ALTER TABLE image_blob
RENAME TO attachment_blob;

ALTER TABLE attachment_blob
MODIFY `data` longblob;
