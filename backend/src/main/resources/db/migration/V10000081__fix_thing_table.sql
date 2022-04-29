ALTER TABLE `thing` MODIFY `note_id` int unsigned;
ALTER TABLE `thing` MODIFY `link_id` int unsigned;

INSERT INTO thing (user_id, created_at, deleted_at, note_id)
SELECT user_id, created_at, deleted_at, id FROM note;

INSERT INTO thing (user_id, created_at, link_id)
SELECT user_id, created_at, id FROM link;
