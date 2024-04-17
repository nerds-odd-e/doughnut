-- Add the audio_id column with specified properties
ALTER TABLE note
ADD COLUMN audio_id INT UNSIGNED DEFAULT NULL;

-- Add a foreign key constraint on the audio_id column of the note table
ALTER TABLE note
ADD CONSTRAINT fk_note_audio_id
FOREIGN KEY (audio_id)
REFERENCES audio(id)
ON DELETE CASCADE
ON UPDATE RESTRICT;
