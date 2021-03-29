package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.NoteEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NoteRepository extends CrudRepository<NoteEntity, Integer> {

    NoteEntity findFirstByParentNoteOrderBySiblingOrderDesc(NoteEntity parentNote);
    NoteEntity findFirstByParentNoteOrderBySiblingOrder(NoteEntity parentNote);
    NoteEntity findFirstByParentNoteAndSiblingOrderLessThanOrderBySiblingOrderDesc(NoteEntity parentNote, Long siblingOrder);
    NoteEntity findFirstByParentNoteAndSiblingOrderGreaterThanOrderBySiblingOrder(NoteEntity parentNote, Long siblingOrder);

    @Query( value = "SELECT note.* from note " + byUserWhereThereIsNoReviewPoint, nativeQuery = true)
    List<NoteEntity> findByUserWhereThereIsNoReviewPoint(@Param("userId") Integer userId);
    @Query( value = "SELECT count(1) as count from note " + byUserWhereThereIsNoReviewPoint, nativeQuery = true)
    int countByUserWhereThereIsNoReviewPoint(@Param("userId") Integer userId);

    NoteEntity findFirstByTitle(String noteTitle);
    List<NoteEntity> findAllByParentNote(NoteEntity parentNote);

    String byUserWhereThereIsNoReviewPoint = " LEFT JOIN ("
                + "     SELECT id, note_id FROM review_point WHERE user_id = :userId"
                + " ) as rp"
                + " ON note.id = rp.note_id "
                + " WHERE note.user_id = :userId "
                + "   AND note.skip_review IS FALSE "
                + "   AND rp.id IS NULL ";

}
