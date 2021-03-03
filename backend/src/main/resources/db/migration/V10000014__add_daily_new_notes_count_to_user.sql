ALTER TABLE user
ADD COLUMN (
    daily_new_notes_count INT UNSIGNED NOT NULL DEFAULT 10,
    space_intervals VARCHAR(100)
);
