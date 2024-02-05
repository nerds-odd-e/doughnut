INSERT INTO hierarchical_note (note_id)
SELECT n.id
FROM note n
LEFT JOIN hierarchical_note hn ON n.id = hn.note_id
WHERE hn.note_id IS NULL;
