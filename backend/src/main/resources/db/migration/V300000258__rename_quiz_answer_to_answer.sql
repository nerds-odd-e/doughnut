-- Rename table quiz_answer to answer and recall_prompt.quiz_answer_id to answer_id.

ALTER TABLE `recall_prompt`
  DROP FOREIGN KEY `fk_recall_prompt_quiz_answer`;

ALTER TABLE `quiz_answer`
  DROP FOREIGN KEY `fk_quiz_answer_confusion_adjusted_memory_tracker`;

RENAME TABLE `quiz_answer` TO `answer`;

ALTER TABLE `answer`
  RENAME INDEX `idx_quiz_answer_created_at` TO `idx_answer_created_at`,
  RENAME INDEX `fk_quiz_answer_confusion_adjusted_memory_tracker` TO `fk_answer_confusion_adjusted_memory_tracker`;

ALTER TABLE `answer`
  ADD CONSTRAINT `fk_answer_confusion_adjusted_memory_tracker`
    FOREIGN KEY (`confusion_adjusted_memory_tracker_id`) REFERENCES `memory_tracker` (`id`) ON DELETE SET NULL;

ALTER TABLE `recall_prompt`
  CHANGE COLUMN `quiz_answer_id` `answer_id` int unsigned DEFAULT NULL,
  RENAME INDEX `fk_recall_prompt_quiz_answer` TO `fk_recall_prompt_answer`;

ALTER TABLE `recall_prompt`
  ADD CONSTRAINT `fk_recall_prompt_answer` FOREIGN KEY (`answer_id`) REFERENCES `answer` (`id`);
