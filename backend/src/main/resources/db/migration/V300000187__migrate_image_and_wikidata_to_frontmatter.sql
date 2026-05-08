-- Migrates note_accessory.image_url / image_id / image_mask and note.wikidata_id
-- into note.content YAML frontmatter.  No schema changes; original columns are kept.
-- Wikidata is on `note` only: `note_accessory` is LEFT JOINed so wikidata-only notes
-- (no accessory row yet) are still updated.
--
-- image_id (resolved to /attachments/images/{id}/{name}) takes priority over image_url,
-- mirroring NoteAccessory.getUrlOfImage().  Only non-blank values are written.

UPDATE `note` `n`
INNER JOIN (
  SELECT
    `x`.`note_id`,
    `x`.`yaml_addition`,
    `x`.`raw`,
    `x`.`norm`,
    IF(
      LENGTH(`x`.`norm`) > 0 AND RIGHT(`x`.`norm`, 1) = CHAR(10),
      `x`.`norm`,
      CONCAT(`x`.`norm`, CHAR(10))
    ) AS `padded`,
    SUBSTRING_INDEX(`x`.`norm`, CHAR(10), 1) AS `first_line`
  FROM (
    SELECT
      `t`.`note_id`,
      `t`.`yaml_addition`,
      `t`.`raw`,
      REPLACE(REPLACE(`t`.`after_bom`, CONCAT(CHAR(13), CHAR(10)), CHAR(10)), CHAR(13), CHAR(10)) AS `norm`
    FROM (
      SELECT
        `n2`.`id` AS `note_id`,
        -- Build the YAML lines to insert; only include non-blank values.
        CONCAT(
          -- image: resolved attachment path wins over image_url
          IF(
            `img`.`id` IS NOT NULL,
            CONCAT('image: /attachments/images/', `img`.`id`, '/', `img`.`name`, CHAR(10)),
            IF(
              `na`.`image_url` IS NOT NULL AND TRIM(`na`.`image_url`) <> '',
              CONCAT('image: ', TRIM(`na`.`image_url`), CHAR(10)),
              ''
            )
          ),
          -- image_mask
          IF(
            `na`.`image_mask` IS NOT NULL AND TRIM(`na`.`image_mask`) <> '',
            CONCAT('image_mask: ', TRIM(`na`.`image_mask`), CHAR(10)),
            ''
          ),
          -- wikidata_id from note
          IF(
            `n2`.`wikidata_id` IS NOT NULL AND TRIM(`n2`.`wikidata_id`) <> '',
            CONCAT('wikidata_id: ', TRIM(`n2`.`wikidata_id`), CHAR(10)),
            ''
          )
        ) AS `yaml_addition`,
        COALESCE(`n2`.`content`, '') AS `raw`,
        IF(
          LENGTH(COALESCE(`n2`.`content`, '')) >= 3
          AND HEX(LEFT(COALESCE(`n2`.`content`, ''), 1)) = 'EFBBBF',
          SUBSTRING(COALESCE(`n2`.`content`, ''), 2),
          COALESCE(`n2`.`content`, '')
        ) AS `after_bom`
      FROM `note` `n2`
      LEFT JOIN `note_accessory` `na` ON `na`.`note_id` = `n2`.`id`
      LEFT JOIN `image` `img` ON `img`.`id` = `na`.`image_id`
      -- At least one of: accessory-backed image fields, or note.wikidata_id.
      WHERE
        (
          `na`.`id` IS NOT NULL
          AND (
            `na`.`image_id` IS NOT NULL
            OR (`na`.`image_url` IS NOT NULL AND TRIM(`na`.`image_url`) <> '')
            OR (`na`.`image_mask` IS NOT NULL AND TRIM(`na`.`image_mask`) <> '')
          )
        )
        OR (`n2`.`wikidata_id` IS NOT NULL AND TRIM(`n2`.`wikidata_id`) <> '')
    ) `t`
  ) `x`
  -- Exclude rows where yaml_addition ended up empty (all values were blank).
  WHERE `x`.`yaml_addition` <> ''
) `m` ON `m`.`note_id` = `n`.`id`
SET `n`.`content` = CASE
  -- No leading frontmatter: first line is not exactly `---`. Prepend a fresh
  -- frontmatter block and keep the original body bytes verbatim (BOM/CRLF preserved).
  WHEN `m`.`first_line` <> '---'
  THEN CONCAT('---', CHAR(10), `m`.`yaml_addition`, '---', CHAR(10), CHAR(10), `m`.`raw`)
  -- Empty YAML frontmatter (`---\n---\n…`): insert yaml_addition between the fences.
  WHEN LEFT(SUBSTRING(`m`.`padded`, 5), 4) = CONCAT('---', CHAR(10))
  THEN CONCAT(
    '---', CHAR(10),
    `m`.`yaml_addition`,
    '---', CHAR(10),
    SUBSTRING(SUBSTRING(`m`.`norm`, 5), 5)
  )
  -- Non-empty YAML frontmatter: split on the first `\n---\n` after the opener.
  WHEN LOCATE(CONCAT(CHAR(10), '---', CHAR(10)), SUBSTRING(`m`.`padded`, 5)) > 0
  THEN CONCAT(
    '---', CHAR(10),
    `m`.`yaml_addition`,
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
  ELSE CONCAT('---', CHAR(10), `m`.`yaml_addition`, '---', CHAR(10), CHAR(10), `m`.`norm`)
END;
