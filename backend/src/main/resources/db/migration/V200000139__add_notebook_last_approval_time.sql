ALTER TABLE notebook ADD COLUMN last_approval_time TIMESTAMP NULL;

UPDATE notebook SET last_approval_time = current_timestamp() WHERE approval_status = 'APPROVED';
