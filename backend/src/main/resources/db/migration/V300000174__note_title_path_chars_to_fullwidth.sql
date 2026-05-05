-- Halfwidth : / \ in note titles → fullwidth ：／＼ (U+FF1A, U+FF0F, U+FF3C).
-- Same character length as varchar(150); no schema change.
-- _utf8mb4 literals so replacements are utf8mb4 (CHAR(...) caused binary conversion errors on Cloud SQL).
UPDATE `note`
SET `title` = REPLACE(
  REPLACE(
    REPLACE(`title`, CHAR(58), _utf8mb4'：'), CHAR(47), _utf8mb4'／'),
  CHAR(92), _utf8mb4'＼')
WHERE LOCATE(CHAR(58), `title`) > 0
   OR LOCATE(CHAR(47), `title`) > 0
   OR LOCATE(CHAR(92), `title`) > 0;
