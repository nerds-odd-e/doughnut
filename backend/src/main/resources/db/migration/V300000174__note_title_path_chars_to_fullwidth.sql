-- Halfwidth : / \ in note titles → fullwidth ：／＼ (U+FF1A, U+FF0F, U+FF3C).
-- Same character length as varchar(150); no schema change.
UPDATE `note`
SET `title` = REPLACE(
  REPLACE(
    REPLACE(`title`, CHAR(58), CHAR(65306)), CHAR(47), CHAR(65295)),
  CHAR(92), CHAR(65340))
WHERE LOCATE(CHAR(58), `title`) > 0
   OR LOCATE(CHAR(47), `title`) > 0
   OR LOCATE(CHAR(92), `title`) > 0;
