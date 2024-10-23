ALTER TABLE note DROP FOREIGN KEY fk_note_audio_id;

-- Remove the audio_id column from the note table
ALTER TABLE note DROP COLUMN audio_id;

-- Drop the foreign key constraint on the audio_id column in the note_accessory table
ALTER TABLE note_accessory DROP FOREIGN KEY note_accessory_ibfk_3;

-- Remove the audio_id column from the note_accessory table
ALTER TABLE note_accessory DROP COLUMN audio_id;

-- Drop the audio table
DROP TABLE audio;
