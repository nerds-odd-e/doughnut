package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.NoteContentEntity;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.OwnershipEntity;
import com.odde.doughnut.entities.UserEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NoteRepository extends CrudRepository<NoteEntity, Integer> {
    @Query( value = "SELECT note.* from note " + byUserWhereThereIsNoReviewPoint, nativeQuery = true)
    List<NoteEntity> findByUserWhereThereIsNoReviewPoint(@Param("userId") Integer userId);

    @Query( value = "SELECT count(1) as count from note " + byUserWhereThereIsNoReviewPoint, nativeQuery = true)
    int countByUserWhereThereIsNoReviewPoint(@Param("userId") Integer userId);

    @Query(nativeQuery = true, value = "SELECT nt.* from note as nt JOIN ownership os ON nt.ownership_id = os.id LEFT JOIN notes_closure nc ON nt.id=nc.note_id WHERE nc.id is NULL and os.id = :ownership")
    List<NoteEntity> orphanedNotesOf(@Param("ownership") OwnershipEntity ownershipEntity);

    @Query( value = "SELECT note.* from note where title = :noteTitle limit 1", nativeQuery = true)
    NoteEntity findFirstByTitle(@Param("noteTitle") String noteTitle);

    @Query( value = "SELECT note.* from note " + byAncestorWhereThereIsNoReviewPoint, nativeQuery = true)
    List<NoteEntity> findByAncestorWhereThereIsNoReviewPoint(@Param("userId") Integer userId, @Param("ancestorId") Integer ancestorId);

    @Query( value = "SELECT count(1) as count from note " + joinClosure + " WHERE note.id in :noteIds", nativeQuery = true)
    int countByAncestorAndInTheList(@Param("ancestorId") Integer ancestorId, @Param("noteIds") List<Integer> noteIds);

    String whereThereIsNoReviewPoint = " LEFT JOIN ("
            + "     SELECT id, note_id FROM review_point WHERE user_id = :userId"
            + " ) as rp"
            + " ON note.id = rp.note_id "
            + " WHERE note.skip_review IS FALSE "
            + "   AND rp.id IS NULL ";

    String byUserWhereThereIsNoReviewPoint = whereThereIsNoReviewPoint
                + " AND note.user_id = :userId ";

    String joinClosure = " JOIN notes_closure ON notes_closure.note_id = note.id "
            + "   AND notes_closure.ancestor_id = :ancestorId ";

    String byAncestorWhereThereIsNoReviewPoint = joinClosure
            + whereThereIsNoReviewPoint;

}
