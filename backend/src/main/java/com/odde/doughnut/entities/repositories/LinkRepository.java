package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.LinkEntity;
import com.odde.doughnut.entities.NoteEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LinkRepository extends CrudRepository<LinkEntity, Integer> {
    @Query( value = "SELECT link.* from link " + byUserWhereThereIsNoReviewPoint, nativeQuery = true)
    List<LinkEntity> findByUserWhereThereIsNoReviewPoint(@Param("userId") Integer userId);

    @Query( value = "SELECT link.* from link " + byAncestorWhereThereIsNoReviewPoint, nativeQuery = true)
    List<LinkEntity> findByAncestorWhereThereIsNoReviewPoint(@Param("userId") Integer userId, @Param("ancestorId") Integer ancestorId);

    String whereThereIsNoReviewPoint = " LEFT JOIN review_point rp"
            + " ON link.id = rp.link_id "
            + "   AND rp.user_id = :userId"
            + " WHERE "
            + "   rp.id IS NULL ";

    String byUserWhereThereIsNoReviewPoint = " JOIN note ON note.id = source_id"
            + "   AND note.user_id = :userId "
            + whereThereIsNoReviewPoint;

    String byAncestorWhereThereIsNoReviewPoint = "JOIN notes_closure ON notes_closure.note_id = source_id "
            + "   AND notes_closure.ancestor_id = :ancestorId "
            + whereThereIsNoReviewPoint;
}
