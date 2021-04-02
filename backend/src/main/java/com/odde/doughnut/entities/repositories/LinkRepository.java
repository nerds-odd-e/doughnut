package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.LinkEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LinkRepository extends CrudRepository<LinkEntity, Integer> {
    @Query( value = "SELECT link.* from link " + byOwnershipWhereThereIsNoReviewPoint, nativeQuery = true)
    List<LinkEntity> findByOwnershipWhereThereIsNoReviewPoint(@Param("userId") Integer userId, @Param("ownershipId") Integer ownershipId);

    @Query( value = "SELECT link.* from link " + byAncestorWhereThereIsNoReviewPoint, nativeQuery = true)
    List<LinkEntity> findByAncestorWhereThereIsNoReviewPoint(@Param("userId") Integer userId, @Param("ancestorId") Integer ancestorId);

    String whereThereIsNoReviewPoint = " LEFT JOIN review_point rp"
            + " ON link.id = rp.link_id "
            + "   AND rp.user_id = :userId"
            + " WHERE "
            + "   rp.id IS NULL ";

    String byOwnershipWhereThereIsNoReviewPoint = " JOIN note ON note.id = source_id"
            + "   AND note.ownership_id = :ownershipId "
            + whereThereIsNoReviewPoint;

    String byAncestorWhereThereIsNoReviewPoint = "JOIN notes_closure ON notes_closure.note_id = source_id "
            + "   AND notes_closure.ancestor_id = :ancestorId "
            + whereThereIsNoReviewPoint;
}
