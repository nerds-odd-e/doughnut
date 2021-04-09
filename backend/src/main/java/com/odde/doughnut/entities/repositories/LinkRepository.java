package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.UserEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LinkRepository extends CrudRepository<Link, Integer> {
    @Query( value = "SELECT link.* from link " + byOwnershipWhereThereIsNoReviewPoint, nativeQuery = true)
    List<Link> findByOwnershipWhereThereIsNoReviewPoint(@Param("userEntity") UserEntity userEntity);

    @Query( value = "SELECT link.* from link " + byAncestorWhereThereIsNoReviewPoint, nativeQuery = true)
    List<Link> findByAncestorWhereThereIsNoReviewPoint(@Param("userEntity") UserEntity userEntity, @Param("ancestor") Note ancestor);

    String whereThereIsNoReviewPoint = " LEFT JOIN review_point rp"
            + " ON link.id = rp.link_id "
            + "   AND rp.user_id = :userEntity"
            + " WHERE "
            + "   rp.id IS NULL ";

    String byOwnershipWhereThereIsNoReviewPoint = " JOIN note ON note.id = source_id"
            + " JOIN notebook ON notebook.id = note.notebook_id "
            + "   AND notebook.ownership_id = :#{#userEntity.ownershipEntity.id} "
            + whereThereIsNoReviewPoint;

    String byAncestorWhereThereIsNoReviewPoint = "JOIN notes_closure ON notes_closure.note_id = source_id "
            + "   AND notes_closure.ancestor_id = :ancestor "
            + whereThereIsNoReviewPoint;
}
