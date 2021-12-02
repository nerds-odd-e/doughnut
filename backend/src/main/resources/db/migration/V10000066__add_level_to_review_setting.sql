ALTER TABLE review_setting ADD level TINYINT;
UPDATE review_setting SET level=0;
ALTER TABLE review_setting MODIFY level TINYINT NOT NULL DEFAULT 0;
ALTER TABLE review_setting ADD INDEX review_setting_level_idx(level);

ALTER TABLE note DROP COLUMN `level`;
