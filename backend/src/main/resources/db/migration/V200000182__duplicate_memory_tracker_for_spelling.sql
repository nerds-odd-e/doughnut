INSERT INTO memory_tracker (
    note_id,
    user_id,
    last_recalled_at,
    next_recall_at,
    assimilated_at,
    repetition_count,
    forgetting_curve_index,
    removed_from_tracking,
    spelling
)
SELECT
    mt.note_id,
    mt.user_id,
    mt.last_recalled_at,
    mt.next_recall_at,
    mt.assimilated_at,
    mt.repetition_count,
    mt.forgetting_curve_index,
    mt.removed_from_tracking,
    true as spelling
FROM memory_tracker mt
JOIN note n ON mt.note_id = n.id
WHERE n.remember_spelling = true
AND mt.spelling = false;
