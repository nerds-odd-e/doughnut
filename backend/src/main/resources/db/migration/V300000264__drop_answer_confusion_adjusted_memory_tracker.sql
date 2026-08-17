ALTER TABLE `answer`
  DROP FOREIGN KEY `fk_answer_confusion_adjusted_memory_tracker`,
  DROP COLUMN `confusion_adjusted_memory_tracker_id`;
