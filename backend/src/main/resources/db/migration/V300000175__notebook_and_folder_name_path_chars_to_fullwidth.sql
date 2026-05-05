-- Halfwidth : / \ in notebook and folder names → fullwidth ：／＼ (same mapping as note titles).
UPDATE `notebook`
SET `name` = REPLACE(
  REPLACE(
    REPLACE(`name`, CHAR(58), CHAR(65306)), CHAR(47), CHAR(65295)),
  CHAR(92), CHAR(65340))
WHERE LOCATE(CHAR(58), `name`) > 0
   OR LOCATE(CHAR(47), `name`) > 0
   OR LOCATE(CHAR(92), `name`) > 0;

UPDATE `folder`
SET `name` = REPLACE(
  REPLACE(
    REPLACE(`name`, CHAR(58), CHAR(65306)), CHAR(47), CHAR(65295)),
  CHAR(92), CHAR(65340))
WHERE LOCATE(CHAR(58), `name`) > 0
   OR LOCATE(CHAR(47), `name`) > 0
   OR LOCATE(CHAR(92), `name`) > 0;
