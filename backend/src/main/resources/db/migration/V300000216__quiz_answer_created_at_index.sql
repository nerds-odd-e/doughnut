-- Supports recent-recall user selection filtered by answered-recall time window.
CREATE INDEX `idx_quiz_answer_created_at` ON `quiz_answer` (`created_at`);
