ALTER TABLE `recall_prompt` ADD COLUMN `memory_tracker_id` int unsigned NOT NULL;

ALTER TABLE `recall_prompt` 
ADD CONSTRAINT `fk_recall_prompt_memory_tracker` 
FOREIGN KEY (`memory_tracker_id`) REFERENCES `memory_tracker` (`id`) ON DELETE CASCADE;
