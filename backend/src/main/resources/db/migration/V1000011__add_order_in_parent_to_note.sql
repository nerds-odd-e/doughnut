ALTER TABLE note
    ADD COLUMN sibling_order BIGINT NOT NULL DEFAULT 1,
    ADD INDEX (parent_id, sibling_order);
