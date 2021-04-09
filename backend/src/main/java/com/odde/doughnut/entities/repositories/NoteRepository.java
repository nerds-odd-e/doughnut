package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.UserEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NoteRepository extends CrudRepository<Note, Integer> {
    @Query( value = "SELECT note.* from note " + byOwnershipWhereThereIsNoReviewPoint, nativeQuery = true)
    List<Note> findByOwnershipWhereThereIsNoReviewPoint(@Param("userEntity") UserEntity userEntity);

    @Query( value = "SELECT count(1) as count from note " + byOwnershipWhereThereIsNoReviewPoint, nativeQuery = true)
    int countByOwnershipWhereThereIsNoReviewPoint(@Param("userEntity") UserEntity userEntity);

    @Query( value = "SELECT note.* from note where title = :noteTitle limit 1", nativeQuery = true)
    Note findFirstByTitle(@Param("noteTitle") String noteTitle);

    @Query( value = "SELECT note.* from note " + byAncestorWhereThereIsNoReviewPoint, nativeQuery = true)
    List<Note> findByAncestorWhereThereIsNoReviewPoint(@Param("userEntity") UserEntity userEntity, @Param("ancestor") Note ancestor);

    @Query( value = "SELECT count(1) as count from note " + byAncestorWhereThereIsNoReviewPoint, nativeQuery = true)
    int countByAncestorWhereThereIsNoReviewPoint(@Param("userEntity") UserEntity userEntity, @Param("ancestor") Note ancestor);

    @Query( value = "SELECT count(1) as count from note " + joinClosure + " WHERE note.id in :noteIds", nativeQuery = true)
    int countByAncestorAndInTheList(@Param("ancestor") Note ancestor, @Param("noteIds") List<Integer> noteIds);

    String whereThereIsNoReviewPoint = " LEFT JOIN review_point rp"
            + " ON note.id = rp.note_id "
            + "   AND rp.user_id = :#{#userEntity.id} "
            + " WHERE note.skip_review IS FALSE "
            + "   AND rp.id IS NULL ";

    String byOwnershipWhereThereIsNoReviewPoint = "JOIN notebook ON notebook.id = note.notebook_id "
            + whereThereIsNoReviewPoint
            + " AND notebook.ownership_id = :#{#userEntity.ownershipEntity.id} ";

    String joinClosure = " JOIN notes_closure ON notes_closure.note_id = note.id "
            + "   AND notes_closure.ancestor_id = :ancestor ";

    String byAncestorWhereThereIsNoReviewPoint = joinClosure
            + whereThereIsNoReviewPoint;
}
