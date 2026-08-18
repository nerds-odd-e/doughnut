-- TIMESTAMP cannot store last_recalled_at + 876000 hours (past 2038). DATETIME can.
-- Session TZ is UTC (JDBC connectionTimeZone=UTC), so existing wall-clock values are preserved.
ALTER TABLE `memory_tracker`
  MODIFY COLUMN `last_recalled_at` datetime NOT NULL,
  MODIFY COLUMN `next_recall_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP;
