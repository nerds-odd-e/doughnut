package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface LinkRepository extends CrudRepository<Link, Integer> {

  String selectLinkWithLevelFromNotes =
      ", GREATEST(source.level, target.level) as level from link "
          + "INNER JOIN ("
          + " SELECT sourceNote.id, srs.level as level FROM note sourceNote LEFT JOIN review_setting srs ON sourceNote.master_review_setting_id = srs.id) as source "
          + " ON source.id = link.source_id "
          + "INNER JOIN ("
          + " SELECT targetNote.id, trs.level as level FROM note targetNote LEFT JOIN review_setting trs ON targetNote.master_review_setting_id = trs.id) as target "
          + " ON target.id = link.target_id ";

  @Query(
      value = "SELECT count(1) from link " + byOwnershipWhereThereIsNoReviewPoint,
      nativeQuery = true)
  int countByOwnershipWhereThereIsNoReviewPoint(@Param("user") User user);

  @Query(
      value = "SELECT count(1) from link " + byAncestorWhereThereIsNoReviewPoint,
      nativeQuery = true)
  int countByAncestorWhereThereIsNoReviewPoint(
      @Param("user") User user, @Param("ancestor") Note ancestor);

  String whereThereIsNoReviewPoint =
      " LEFT JOIN review_point rp"
          + " ON link.id = rp.link_id "
          + "   AND rp.user_id = :user"
          + " WHERE "
          + "   rp.id IS NULL ";

  String orderByDate = " ORDER BY level, link.created_at";

  String byOwnershipWhereThereIsNoReviewPoint =
      " JOIN note ON note.id = source_id"
          + " JOIN notebook ON notebook.id = note.notebook_id "
          + "   AND notebook.ownership_id = :#{#user.ownership.id} "
          + whereThereIsNoReviewPoint;

  String byAncestorWhereThereIsNoReviewPoint =
      "JOIN notes_closure ON notes_closure.note_id = source_id "
          + "   AND notes_closure.ancestor_id = :ancestor "
          + whereThereIsNoReviewPoint;

  @Query(value = "SELECT link.* FROM link where id in (:ids)", nativeQuery = true)
  Stream<Link> findAllByIds(String[] ids);
}
