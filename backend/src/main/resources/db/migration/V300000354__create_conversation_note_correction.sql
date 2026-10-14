CREATE TABLE conversation_note_correction (
  id int unsigned NOT NULL AUTO_INCREMENT,
  source_message_id int unsigned NOT NULL,
  note_id int unsigned NOT NULL,
  response_id varchar(64) NOT NULL,
  tool_call_id varchar(128) NOT NULL,
  field varchar(16) NOT NULL,
  proposed_value mediumtext NOT NULL,
  reason text NOT NULL,
  base_revision varchar(40) DEFAULT NULL,
  decision varchar(16) NOT NULL DEFAULT 'PENDING',
  reviewed_value mediumtext,
  decided_by int unsigned DEFAULT NULL,
  decided_at timestamp(6) NULL DEFAULT NULL,
  accepted_revision varchar(40) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY unique_correction_tool_call (source_message_id, response_id, tool_call_id),
  CONSTRAINT fk_correction_source_message FOREIGN KEY (source_message_id)
    REFERENCES conversation_message (id) ON DELETE CASCADE,
  CONSTRAINT fk_correction_note FOREIGN KEY (note_id)
    REFERENCES note (id) ON DELETE CASCADE,
  CONSTRAINT fk_correction_reviewer FOREIGN KEY (decided_by)
    REFERENCES user (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
