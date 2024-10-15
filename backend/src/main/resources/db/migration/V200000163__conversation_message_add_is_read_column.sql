ALTER TABLE conversation_message ADD COLUMN is_read BOOLEAN DEFAULT FALSE;

update conversation_message set is_read = true; 