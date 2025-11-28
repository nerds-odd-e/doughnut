-- Step 1: Add nullable column
ALTER TABLE `recall_prompt` ADD COLUMN `memory_tracker_id` int unsigned DEFAULT NULL;

-- Step 2: Backfill data - find the first memory tracker for each note
UPDATE `recall_prompt` rp
INNER JOIN `predefined_question` pq ON rp.predefined_question_id = pq.id
INNER JOIN (
    SELECT note_id, MIN(id) as first_memory_tracker_id
    FROM `memory_tracker`
    WHERE removed_from_tracking = 0
    GROUP BY note_id
) mt ON pq.note_id = mt.note_id
SET rp.memory_tracker_id = mt.first_memory_tracker_id;

-- Step 3: Make NOT NULL
ALTER TABLE `recall_prompt` MODIFY COLUMN `memory_tracker_id` int unsigned NOT NULL;

-- Step 4: Add foreign key
ALTER TABLE `recall_prompt` 
ADD CONSTRAINT `fk_recall_prompt_memory_tracker` 
FOREIGN KEY (`memory_tracker_id`) REFERENCES `memory_tracker` (`id`) ON DELETE CASCADE;

