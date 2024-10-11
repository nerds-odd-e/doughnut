ALTER TABLE conversation_detail DROP COLUMN user_type;
ALTER TABLE conversation_detail ADD COLUMN conversation_detail_initiator_id int unsigned;
ALTER TABLE conversation_detail ADD CONSTRAINT fk_conversation_detail_initiator_id FOREIGN KEY (conversation_detail_initiator_id) REFERENCES user (id) ON DELETE CASCADE;
