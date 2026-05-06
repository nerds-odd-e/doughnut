ALTER TABLE `notebook`
  ADD COLUMN `created_at` timestamp NULL DEFAULT NULL AFTER `updated_at`;

UPDATE `notebook` n
LEFT JOIN (
  SELECT n1.`notebook_id`, n1.`created_at`
  FROM `note` n1
  INNER JOIN (
    SELECT `notebook_id`, MIN(`id`) AS first_note_id
    FROM `note`
    WHERE `notebook_id` IS NOT NULL
    GROUP BY `notebook_id`
  ) x ON n1.`notebook_id` = x.`notebook_id` AND n1.`id` = x.first_note_id
) first_note ON first_note.`notebook_id` = n.`id`
SET n.`created_at` = COALESCE(first_note.`created_at`, n.`updated_at`, CURRENT_TIMESTAMP);

ALTER TABLE `notebook`
  MODIFY COLUMN `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP;
