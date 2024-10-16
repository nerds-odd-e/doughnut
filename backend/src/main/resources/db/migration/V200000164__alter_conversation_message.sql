
update conversation_message set is_read = true; 
ALTER TABLE conversation_message MODIFY COLUMN is_read BOOLEAN NOT NULL DEFAULT FALSE;
