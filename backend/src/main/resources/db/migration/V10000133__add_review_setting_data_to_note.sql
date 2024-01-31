ALTER TABLE note
ADD COLUMN remember_spelling tinyint NOT NULL DEFAULT '0';

ALTER TABLE note
ADD COLUMN skip_review tinyint NOT NULL DEFAULT '0';

ALTER TABLE note
ADD COLUMN level tinyint NOT NULL DEFAULT '0';

UPDATE note n
INNER JOIN review_setting rs ON n.master_review_setting_id = rs.id
SET n.remember_spelling = rs.remember_spelling, n.skip_review = rs.skip_review, n.level = rs.level;

