UPDATE quiz_question q
INNER JOIN thing l ON l.id = q.category_link_id
SET q.category_link_id = l.note_id
