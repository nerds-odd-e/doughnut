ALTER TABLE image
CHANGE image_blob_id attachment_blob_id INT unsigned;

ALTER TABLE audio
DROP FOREIGN KEY FK_audio_audio_blob_id;

-- Drop the audio_blob_id column
ALTER TABLE audio
DROP COLUMN audio_blob_id;

-- Add the attachment_blob_id column
ALTER TABLE audio
ADD COLUMN attachment_blob_id INT unsigned;

-- Add the foreign key constraint
ALTER TABLE audio
ADD CONSTRAINT FK_audio_attachment_blob_id
FOREIGN KEY (attachment_blob_id) REFERENCES attachment_blob(id);

drop table audio_blob;
