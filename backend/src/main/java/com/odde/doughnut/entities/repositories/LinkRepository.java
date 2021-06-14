package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LinkRepository extends CrudRepository<Link, Integer> {
    @Query( value = "SELECT link.* from link " + byOwnershipWhereThereIsNoReviewPoint, nativeQuery = true)
    List<Link> findByOwnershipWhereThereIsNoReviewPoint(@Param("user") User user);

    @Query( value = "SELECT link.* from link " + byAncestorWhereThereIsNoReviewPoint, nativeQuery = true)
    List<Link> findByAncestorWhereThereIsNoReviewPoint(@Param("user") User user, @Param("ancestor") Note ancestor);

    String whereThereIsNoReviewPoint = " LEFT JOIN review_point rp"
            + " ON link.id = rp.link_id "
            + "   AND rp.user_id = :user"
            + " WHERE "
            + "   rp.id IS NULL "
            + " ORDER BY link.created_at";

    String byOwnershipWhereThereIsNoReviewPoint = " JOIN note ON note.id = source_id"
            + " JOIN notebook ON notebook.id = note.notebook_id "
            + "   AND notebook.ownership_id = :#{#user.ownership.id} "
            + whereThereIsNoReviewPoint;

    String byAncestorWhereThereIsNoReviewPoint = "JOIN notes_closure ON notes_closure.note_id = source_id "
            + "   AND notes_closure.ancestor_id = :ancestor "
            + whereThereIsNoReviewPoint;
}
