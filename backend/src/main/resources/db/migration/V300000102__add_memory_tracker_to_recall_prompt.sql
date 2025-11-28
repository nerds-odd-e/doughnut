-- Step 1: Add nullable column (only if it doesn't exist)
SET @col_exists = (
  SELECT COUNT(*) 
  FROM information_schema.COLUMNS 
  WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'recall_prompt' 
    AND COLUMN_NAME = 'memory_tracker_id'
);

SET @sql = IF(@col_exists = 0,
  'ALTER TABLE `recall_prompt` ADD COLUMN `memory_tracker_id` int unsigned DEFAULT NULL',
  'SET @skip = 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 2: Backfill data - find the first memory tracker for each note (only if column exists and is nullable)
SET @col_is_nullable = (
  SELECT IS_NULLABLE 
  FROM information_schema.COLUMNS 
  WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'recall_prompt' 
    AND COLUMN_NAME = 'memory_tracker_id'
);

UPDATE `recall_prompt` rp
INNER JOIN `predefined_question` pq ON rp.predefined_question_id = pq.id
INNER JOIN (
    SELECT note_id, MIN(id) as first_memory_tracker_id
    FROM `memory_tracker`
    WHERE removed_from_tracking = 0
    GROUP BY note_id
) mt ON pq.note_id = mt.note_id
SET rp.memory_tracker_id = mt.first_memory_tracker_id
WHERE rp.memory_tracker_id IS NULL
  AND @col_is_nullable = 'YES';

-- Step 2b: Delete orphaned recall_prompt rows that don't have a matching memory_tracker
-- (These are likely orphaned data that can't be properly associated)
DELETE rp FROM `recall_prompt` rp
LEFT JOIN `predefined_question` pq ON rp.predefined_question_id = pq.id
LEFT JOIN (
    SELECT note_id, MIN(id) as first_memory_tracker_id
    FROM `memory_tracker`
    WHERE removed_from_tracking = 0
    GROUP BY note_id
) mt ON pq.note_id = mt.note_id
WHERE rp.memory_tracker_id IS NULL
  AND @col_is_nullable = 'YES';

-- Step 3: Make NOT NULL (only if it's still nullable and no NULL values exist)
SET @col_is_nullable = (
  SELECT IS_NULLABLE 
  FROM information_schema.COLUMNS 
  WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'recall_prompt' 
    AND COLUMN_NAME = 'memory_tracker_id'
);

SET @null_count = (
  SELECT COUNT(*) 
  FROM `recall_prompt` 
  WHERE `memory_tracker_id` IS NULL
);

SET @sql = IF(@col_is_nullable = 'YES' AND @null_count = 0,
  'ALTER TABLE `recall_prompt` MODIFY COLUMN `memory_tracker_id` int unsigned NOT NULL',
  'SET @skip = 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 4: Add foreign key (only if it doesn't exist)
SET @fk_exists = (
  SELECT COUNT(*) 
  FROM information_schema.TABLE_CONSTRAINTS 
  WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'recall_prompt' 
    AND CONSTRAINT_NAME = 'fk_recall_prompt_memory_tracker'
);

SET @sql = IF(@fk_exists = 0,
  'ALTER TABLE `recall_prompt` 
   ADD CONSTRAINT `fk_recall_prompt_memory_tracker` 
   FOREIGN KEY (`memory_tracker_id`) REFERENCES `memory_tracker` (`id`) ON DELETE CASCADE',
  'SET @skip = 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
