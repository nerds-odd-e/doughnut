-- Preserve millisecond save clocks for amendment eligibility comparisons.
-- Existing whole-second values cannot be recovered; freeze active batches once.
ALTER TABLE `notebook_git_binding`
  MODIFY COLUMN `amendment_last_changed_at` timestamp(3) NULL;

UPDATE `notebook_git_binding`
SET
  `amendment_head` = NULL,
  `amendment_note_id` = NULL,
  `amendment_last_changed_at` = NULL
WHERE `amendment_head` IS NOT NULL
   OR `amendment_note_id` IS NOT NULL
   OR `amendment_last_changed_at` IS NOT NULL;
