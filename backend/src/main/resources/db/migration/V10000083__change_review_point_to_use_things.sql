UPDATE review_point
INNER JOIN thing ON thing.note_id = review_point.note_id OR thing.link_id = review_point.link_id
SET thing_id = thing.id;
