ALTER TABLE conversation DROP COLUMN marker;
ALTER TABLE conversation DROP COLUMN message;

ALTER TABLE conversation_detail CHANGE COLUMN conversation_detail_initiator_id sender int unsigned;

RENAME TABLE conversation_detail TO conversation_message;
