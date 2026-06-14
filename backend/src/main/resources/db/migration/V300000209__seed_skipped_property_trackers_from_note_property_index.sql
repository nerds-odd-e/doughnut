-- Seeds skipped property memory trackers for indexed frontmatter keys that still have no
-- property-level tracker. Idempotent: safe to run when V300000206/208 already seeded some rows.

INSERT INTO memory_tracker (
  user_id,
  note_id,
  spelling,
  property_key,
  removed_from_tracking,
  assimilated_at,
  last_recalled_at,
  next_recall_at,
  forgetting_curve_index,
  recall_count
)
SELECT
  o.user_id,
  npi.note_id,
  0,
  npi.property_key,
  1,
  UTC_TIMESTAMP(),
  UTC_TIMESTAMP(),
  UTC_TIMESTAMP(),
  100.0,
  0
FROM note_property_index npi
INNER JOIN note n ON n.id = npi.note_id AND n.deleted_at IS NULL
INNER JOIN notebook nb ON nb.id = n.notebook_id AND nb.deleted_at IS NULL
INNER JOIN ownership o ON o.id = nb.ownership_id AND o.user_id IS NOT NULL
WHERE LOWER(TRIM(npi.property_key)) <> 'example of'
  AND NOT (
    LOWER(TRIM(npi.property_key)) LIKE 'example of %'
    AND CAST(TRIM(SUBSTRING(LOWER(TRIM(npi.property_key)), 12)) AS UNSIGNED) >= 2
  )
  AND LOWER(TRIM(npi.property_key)) NOT IN (
    'image',
    'image_mask',
    'imagemask',
    'wikidata_id',
    'wikidataid',
    'url',
    'title_pattern',
    'titlepattern',
    'question_generation_instruction',
    'questiongenerationinstruction',
    'type',
    'relation',
    'source',
    'target'
  )
  AND NOT EXISTS (
    SELECT 1
    FROM memory_tracker mt
    WHERE mt.user_id = o.user_id
      AND mt.note_id = npi.note_id
      AND mt.spelling = 0
      AND mt.property_key = npi.property_key
      AND mt.deleted_at IS NULL
  );
