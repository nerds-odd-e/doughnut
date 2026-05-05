-- Halfwidth : / \ in notebook and folder names → fullwidth ：／＼ (same mapping as note titles).
-- _utf8mb4 literals so replacements are utf8mb4 (CHAR(...) for fullwidth caused binary conversion errors on Cloud SQL).
UPDATE `notebook`
SET `name` = REPLACE(
  REPLACE(
    REPLACE(`name`, CHAR(58), _utf8mb4'：'), CHAR(47), _utf8mb4'／'),
  CHAR(92), _utf8mb4'＼')
WHERE LOCATE(CHAR(58), `name`) > 0
   OR LOCATE(CHAR(47), `name`) > 0
   OR LOCATE(CHAR(92), `name`) > 0;

UPDATE `folder`
SET `name` = REPLACE(
  REPLACE(
    REPLACE(`name`, CHAR(58), _utf8mb4'：'), CHAR(47), _utf8mb4'／'),
  CHAR(92), _utf8mb4'＼')
WHERE LOCATE(CHAR(58), `name`) > 0
   OR LOCATE(CHAR(47), `name`) > 0
   OR LOCATE(CHAR(92), `name`) > 0;
