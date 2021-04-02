package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.LinkEntity;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.UserEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LinkRepository extends CrudRepository<LinkEntity, Integer> {
    @Query( value = "SELECT link.* from link " + byOwnershipWhereThereIsNoReviewPoint, nativeQuery = true)
    List<LinkEntity> findByOwnershipWhereThereIsNoReviewPoint(@Param("userEntity") UserEntity userEntity);

    @Query( value = "SELECT link.* from link " + byAncestorWhereThereIsNoReviewPoint, nativeQuery = true)
    List<LinkEntity> findByAncestorWhereThereIsNoReviewPoint(@Param("userEntity") UserEntity userEntity, @Param("ancestor") NoteEntity ancestor);

    String whereThereIsNoReviewPoint = " LEFT JOIN review_point rp"
            + " ON link.id = rp.link_id "
            + "   AND rp.user_id = :userEntity"
            + " WHERE "
            + "   rp.id IS NULL ";

    String byOwnershipWhereThereIsNoReviewPoint = " JOIN note ON note.id = source_id"
            + "   AND note.ownership_id = :#{#userEntity.ownershipEntity.id} "
            + whereThereIsNoReviewPoint;

    String byAncestorWhereThereIsNoReviewPoint = "JOIN notes_closure ON notes_closure.note_id = source_id "
            + "   AND notes_closure.ancestor_id = :ancestor "
            + whereThereIsNoReviewPoint;
}
