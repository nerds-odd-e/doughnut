package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.NoteEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NoteRepository extends CrudRepository<NoteEntity, Integer> {

    @Query(
            value = "WITH RECURSIVE ancestors(id, parent_id, lvl) AS ("
                    + "   SELECT thisNote.id, thisNote.parent_id, 1 AS lvl "
                    + "   FROM note thisNote "
                    + "   WHERE thisNote.id = :noteId "
                    + "   UNION ALL "
                    + "   SELECT parent.id, parent.parent_id, child.lvl + 1 AS lvl "
                    + "   FROM note parent "
                    + "   JOIN ancestors child "
                    + "   ON parent.id = child.parent_id "
                    + " )"
                    + "SELECT id from ancestors ORDER BY lvl DESC"
            , nativeQuery = true)
    List<NoteEntity> findAncestry(@Param("noteId") Long noteId);

    NoteEntity findFirstByParentNoteOrderBySiblingOrderDesc(NoteEntity parentNote);
    NoteEntity findFirstByParentNoteOrderBySiblingOrder(NoteEntity parentNote);
    NoteEntity findFirstByParentNoteAndSiblingOrderLessThanOrderBySiblingOrderDesc(NoteEntity parentNote, Long siblingOrder);
    NoteEntity findFirstByParentNoteAndSiblingOrderGreaterThanOrderBySiblingOrder(NoteEntity parentNote, Long siblingOrder);

    @Query(
            value = "SELECT note.* from note "
                    + " LEFT JOIN ("
                    + "     SELECT id, note_id FROM review_point WHERE user_id = :userId"
                    + " ) as rp"
                    + " ON note.id = rp.note_id "
                    + " WHERE note.user_id = :userId "
                    + "   AND rp.id IS NULL "
            , nativeQuery = true)
    List<NoteEntity> findByUserWhereThereIsNoReviewPoint(@Param("userId") Integer userId);

}
