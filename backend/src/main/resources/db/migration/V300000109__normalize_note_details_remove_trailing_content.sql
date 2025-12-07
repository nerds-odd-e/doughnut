-- Normalize note details by removing trailing empty lines, <br>, and <p><br></p> tags
-- This matches the frontend normalization logic in normalizeNoteDetails.ts
-- MySQL 8.0+ REGEXP_REPLACE is used to remove trailing content
-- Uses [[:space:]] character class for whitespace matching (POSIX compatible)
-- Uses \n for newline matching (MySQL interprets \n in string literals as newline)

UPDATE `note`
SET `details` = TRIM(
  REGEXP_REPLACE(
    REGEXP_REPLACE(
      REGEXP_REPLACE(
        REGEXP_REPLACE(
          TRIM(COALESCE(`details`, '')),
          '([[:space:]]*<p><br></p>[[:space:]]*)+$', '', 1, 0, 'i'
        ),
        '([[:space:]]*<br>[[:space:]]*)+$', '', 1, 0, 'i'
      ),
      '\n[[:space:]]*$', '', 1, 0
    ),
    '\n+$', '', 1, 0
  )
)
WHERE `details` IS NOT NULL
  AND `details` != '';

