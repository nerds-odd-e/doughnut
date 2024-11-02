ALTER TABLE conversation MODIFY last_ai_assistant_thread_sync TIMESTAMP(3);
ALTER TABLE conversation_message MODIFY created_at TIMESTAMP(3);
