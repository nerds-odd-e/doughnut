-- Convert live property-level dummy sequence skips (UNDERSTANDING, removed from
-- tracking, recall_count = 0, non-empty property_key) into assimilation_sequence_skip
-- rows and soft-delete those trackers.
--
-- No-op everywhere by default (`spring.flyway.placeholders.dummy_property_sequence_skip_convert=1=0`).
-- Enabled (`1=1`) only for the deliberate production conversion deployment.
INSERT INTO assimilation_sequence_skip (user_id, note_id, property_key, skipped_at)
SELECT mt.user_id, mt.note_id, mt.property_key, mt.assimilated_at
FROM memory_tracker mt
WHERE ${dummy_property_sequence_skip_convert}
  AND mt.deleted_at IS NULL
  AND mt.removed_from_tracking IS TRUE
  AND mt.recall_count = 0
  AND mt.type = 'UNDERSTANDING'
  AND mt.property_key <> ''
  AND NOT EXISTS (
    SELECT 1
    FROM assimilation_sequence_skip skip
    WHERE skip.user_id = mt.user_id
      AND skip.note_id = mt.note_id
      AND skip.property_key = mt.property_key
  );

UPDATE memory_tracker mt
SET mt.deleted_at = CURRENT_TIMESTAMP
WHERE ${dummy_property_sequence_skip_convert}
  AND mt.deleted_at IS NULL
  AND mt.removed_from_tracking IS TRUE
  AND mt.recall_count = 0
  AND mt.type = 'UNDERSTANDING'
  AND mt.property_key <> '';
