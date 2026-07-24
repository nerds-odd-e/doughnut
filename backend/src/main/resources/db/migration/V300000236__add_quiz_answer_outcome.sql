-- Persist AnswerOutcome enum name for durable grade metadata (e.g. OVERLAP threshold exclusion).
ALTER TABLE `quiz_answer`
  ADD COLUMN `outcome` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL;
