--
-- Add audio column to table `note`
--

ALTER TABLE note
ADD COLUMN audio_id INT UNSIGNED NULL,
ADD FOREIGN KEY (audio_id) REFERENCES audio (id) ON DELETE CASCADE ON UPDATE RESTRICT;

