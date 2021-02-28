package com.odde.doughnut.repositories;

import com.odde.doughnut.models.Note;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NoteRepository extends CrudRepository<Note, Integer> {
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
    List<Note> findAncestry(@Param("noteId") Long noteId);
    Note findFirstByParentNoteOrderBySiblingOrderDesc(Note parentNote);
    Note findFirstByParentNoteOrderBySiblingOrder(Note parentNote);
    Note findFirstByParentNoteAndSiblingOrderLessThanOrderBySiblingOrderDesc(Note parentNote, Long siblingOrder);
    Note findFirstByParentNoteAndSiblingOrderGreaterThanOrderBySiblingOrder(Note parentNote, Long siblingOrder);

}
