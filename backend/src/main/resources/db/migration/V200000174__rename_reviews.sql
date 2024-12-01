ALTER TABLE note
RENAME COLUMN skip_review TO skip_memory_tracking;
ALTER TABLE memory_tracker
RENAME COLUMN removed_from_review TO removed_from_tracking;

ALTER TABLE memory_tracker
RENAME COLUMN last_reviewed_at TO last_recalled_at;
ALTER TABLE memory_tracker
RENAME COLUMN next_review_at TO next_recall_at;
ALTER TABLE memory_tracker
RENAME COLUMN initial_reviewed_at TO onboarded_at;
