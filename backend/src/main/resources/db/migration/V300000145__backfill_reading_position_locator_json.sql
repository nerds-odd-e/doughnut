-- Backfill reading_position_locator_json from legacy columns (Track F4).
-- EPUB rows first so any inconsistent dual-populated legacy rows prefer EPUB.

UPDATE book_user_last_read_position AS t
INNER JOIN (
  SELECT
    id,
    IF(
      frag_norm IS NULL OR frag_norm = '',
      JSON_OBJECT('type', 'EpubLocator_Full', 'href', href_part),
      JSON_OBJECT(
        'type',
        'EpubLocator_Full',
        'href',
        href_part,
        'fragment',
        frag_norm
      )
    ) AS new_json
  FROM (
    SELECT
      id,
      IF(hp = 0, te, TRIM(SUBSTRING(te, 1, hp - 1))) AS href_part,
      NULLIF(
        TRIM(
          IF(
            fr IS NOT NULL AND LEFT(fr, 1) = '#',
            SUBSTRING(fr, 2),
            fr
          )
        ),
        ''
      ) AS frag_norm
    FROM (
      SELECT
        id,
        TRIM(IFNULL(epub_locator, '')) AS te,
        LOCATE('#', TRIM(IFNULL(epub_locator, ''))) AS hp,
        IF(
          LOCATE('#', TRIM(IFNULL(epub_locator, ''))) = 0,
          NULL,
          TRIM(
            SUBSTRING(
              TRIM(IFNULL(epub_locator, '')),
              LOCATE('#', TRIM(IFNULL(epub_locator, ''))) + 1
            )
          )
        ) AS fr
      FROM book_user_last_read_position
      WHERE reading_position_locator_json IS NULL
        AND TRIM(IFNULL(epub_locator, '')) <> ''
    ) AS raw
  ) AS parts
  WHERE parts.href_part <> ''
) AS calc ON calc.id = t.id
SET t.reading_position_locator_json = calc.new_json
WHERE t.reading_position_locator_json IS NULL;

UPDATE book_user_last_read_position
SET reading_position_locator_json = JSON_OBJECT(
  'type',
  'PdfLocator_Full',
  'pageIndex',
  page_index,
  'bbox',
  JSON_ARRAY(0, normalized_y, 100, 600)
)
WHERE reading_position_locator_json IS NULL
  AND page_index IS NOT NULL
  AND normalized_y IS NOT NULL
  AND TRIM(IFNULL(epub_locator, '')) = '';
