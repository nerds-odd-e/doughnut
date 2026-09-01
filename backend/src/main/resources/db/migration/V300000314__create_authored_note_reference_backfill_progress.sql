-- Tracks completion of the one-time startup backfill of `authored_note_reference` rows for
-- pre-existing notes, separately from the reference rows themselves. A single row (id = 1) whose
-- last_processed_note_id is the resumable high-water mark for bounded per-batch commits, and whose
-- completed_at (once set) lets a restart skip the whole backfill in one O(1) read — so a note that
-- legitimately has zero authored references is never mistaken for "not yet backfilled" and
-- rescanned on every startup.

CREATE TABLE `authored_note_reference_backfill_progress` (
  `id` tinyint unsigned NOT NULL,
  `last_processed_note_id` int unsigned DEFAULT NULL,
  `completed_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `authored_note_reference_backfill_progress` (`id`) VALUES (1);
