-- Record-time sessions always store feedback; remove legacy null-feedback items
-- and orphaned sessions, then enforce NOT NULL.

DELETE FROM `session_item`
WHERE `feedback_score` IS NULL
   OR `feedback_recorded_at` IS NULL;

DELETE `learning_session`
FROM `learning_session`
LEFT JOIN `session_item`
  ON `session_item`.`learning_session_id` = `learning_session`.`id`
WHERE `session_item`.`id` IS NULL;

ALTER TABLE `session_item`
  MODIFY COLUMN `feedback_score` int NOT NULL,
  MODIFY COLUMN `feedback_recorded_at` timestamp(3) NOT NULL;
