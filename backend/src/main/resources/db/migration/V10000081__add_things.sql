INSERT INTO thing (user_id, created_at, deleted_at, note_id)
SELECT user_id, created_at, deleted_at, id FROM note;

INSERT INTO thing (user_id, created_at, link_id)
SELECT user_id, created_at, id FROM link;
