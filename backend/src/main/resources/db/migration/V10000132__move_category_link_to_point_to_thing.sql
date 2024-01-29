UPDATE quiz_question q
INNER JOIN link l ON l.id = q.category_link_id
INNER JOIN thing t ON l.id = t.link_id
SET q.category_link_id = t.id
