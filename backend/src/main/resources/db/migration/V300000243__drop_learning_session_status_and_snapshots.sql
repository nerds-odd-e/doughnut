-- Ephemeral request + record-time sessions: drop obsolete commission/status
-- and pre-session snapshot columns.

-- Composite status index backs the user_id FK; add dedicated keys first.
ALTER TABLE `learning_session`
  ADD KEY `idx_learning_session_user` (`user_id`),
  ADD KEY `idx_learning_session_notebook` (`notebook_id`);

ALTER TABLE `learning_session`
  DROP INDEX `idx_learning_session_user_notebook_status`,
  DROP COLUMN `status`,
  DROP COLUMN `commissioned_at`;

ALTER TABLE `session_item`
  DROP COLUMN `pre_session_forgetting_curve_index`,
  DROP COLUMN `pre_session_recall_count`;
