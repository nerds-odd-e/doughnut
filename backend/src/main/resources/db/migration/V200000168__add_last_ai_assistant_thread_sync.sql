ALTER TABLE conversation
ADD COLUMN last_ai_assistant_thread_sync TIMESTAMP NULL DEFAULT NULL;
