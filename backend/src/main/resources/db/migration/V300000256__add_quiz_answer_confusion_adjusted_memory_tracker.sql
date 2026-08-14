ALTER TABLE `quiz_answer`
  ADD COLUMN `confusion_adjusted_memory_tracker_id` int unsigned DEFAULT NULL,
  ADD KEY `fk_quiz_answer_confusion_adjusted_memory_tracker` (`confusion_adjusted_memory_tracker_id`),
  ADD CONSTRAINT `fk_quiz_answer_confusion_adjusted_memory_tracker`
    FOREIGN KEY (`confusion_adjusted_memory_tracker_id`) REFERENCES `memory_tracker` (`id`) ON DELETE SET NULL;
