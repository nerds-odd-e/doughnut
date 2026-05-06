-- Migrates legacy note_accessory.url into note.details YAML frontmatter (url: …), then drops note_accessory.url.
-- Rows: note_accessory joined to note where url is non-null and non-blank after trim.

UPDATE `note` `n`
INNER JOIN (
  SELECT
    `x`.`note_id`,
    `x`.`trim_url`,
    `x`.`raw`,
    `x`.`norm`,
    -- Pad with a trailing LF so the closing `\n---\n` pattern matches even when the
    -- source ends right after `---` with no trailing newline.
    IF(
      LENGTH(`x`.`norm`) > 0 AND RIGHT(`x`.`norm`, 1) = CHAR(10),
      `x`.`norm`,
      CONCAT(`x`.`norm`, CHAR(10))
    ) AS `padded`,
    SUBSTRING_INDEX(`x`.`norm`, CHAR(10), 1) AS `first_line`
  FROM (
    SELECT
      `t`.`note_id`,
      `t`.`trim_url`,
      `t`.`raw`,
      REPLACE(REPLACE(`t`.`after_bom`, CONCAT(CHAR(13), CHAR(10)), CHAR(10)), CHAR(13), CHAR(10)) AS `norm`
    FROM (
      SELECT
        `n2`.`id` AS `note_id`,
        TRIM(`na`.`url`) AS `trim_url`,
        COALESCE(`n2`.`details`, '') AS `raw`,
        IF(
          LENGTH(COALESCE(`n2`.`details`, '')) >= 3
          AND HEX(LEFT(COALESCE(`n2`.`details`, ''), 1)) = 'EFBBBF',
          SUBSTRING(COALESCE(`n2`.`details`, ''), 2),
          COALESCE(`n2`.`details`, '')
        ) AS `after_bom`
      FROM `note` `n2`
      INNER JOIN `note_accessory` `na` ON `na`.`note_id` = `n2`.`id`
      WHERE `na`.`url` IS NOT NULL AND TRIM(`na`.`url`) <> ''
    ) `t`
  ) `x`
) `m` ON `m`.`note_id` = `n`.`id`
SET `n`.`details` = CASE
  -- No leading frontmatter: first line is not exactly `---`. Prepend a fresh
  -- frontmatter block and keep the original body bytes verbatim (BOM/CRLF preserved).
  WHEN `m`.`first_line` <> '---'
  THEN CONCAT('---', CHAR(10), 'url: ', `m`.`trim_url`, CHAR(10), '---', CHAR(10), CHAR(10), `m`.`raw`)
  -- Empty YAML frontmatter (`---\n---\n…`): insert `url:` line between the fences.
  -- Detection runs against `padded` so `---\n---` (no trailing LF) is also recognized.
  -- Body is extracted from `norm` so we never carry the synthetic padding LF into output.
  WHEN LEFT(SUBSTRING(`m`.`padded`, 5), 4) = CONCAT('---', CHAR(10))
  THEN CONCAT(
    '---', CHAR(10),
    'url: ', `m`.`trim_url`, CHAR(10),
    '---', CHAR(10),
    SUBSTRING(SUBSTRING(`m`.`norm`, 5), 5)
  )
  -- Non-empty YAML frontmatter: split on the first `\n---\n` after the opener.
  -- Locate runs against `padded` so a closer that ends without a trailing LF still
  -- matches; yaml_text and body are sliced from `norm` to preserve original bytes.
  WHEN LOCATE(CONCAT(CHAR(10), '---', CHAR(10)), SUBSTRING(`m`.`padded`, 5)) > 0
  THEN CONCAT(
    '---', CHAR(10),
    'url: ', `m`.`trim_url`, CHAR(10),
    SUBSTRING(
      SUBSTRING(`m`.`norm`, 5),
      1,
      LOCATE(CONCAT(CHAR(10), '---', CHAR(10)), SUBSTRING(`m`.`padded`, 5)) - 1
    ),
    CHAR(10), '---', CHAR(10),
    SUBSTRING(
      SUBSTRING(`m`.`norm`, 5),
      LOCATE(CONCAT(CHAR(10), '---', CHAR(10)), SUBSTRING(`m`.`padded`, 5)) + 5
    )
  )
  -- First line is exactly `---` but no closing fence ever appears: treat as no
  -- frontmatter and prepend a fresh block, keeping the normalized text as body.
  ELSE CONCAT('---', CHAR(10), 'url: ', `m`.`trim_url`, CHAR(10), '---', CHAR(10), CHAR(10), `m`.`norm`)
END;

ALTER TABLE `note_accessory` DROP COLUMN `url`;
