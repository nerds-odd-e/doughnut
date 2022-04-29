package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Thing;
import com.odde.doughnut.entities.User;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface ThingRepository extends CrudRepository<Thing, Integer> {

  @Query(value = "SELECT thing.* " + selectThings + orderByDate, nativeQuery = true)
  Stream<Thing> findByOwnershipWhereThereIsNoReviewPoint(@Param("user") User user);

  @Query(value = "SELECT thing.* " + selectThingsByAncestor + orderByDate, nativeQuery = true)
  Stream<Thing> findByAncestorWhereThereIsNoReviewPoint(
      @Param("user") User user, @Param("ancestor") Note ancestor);

  String whereThereIsNoReviewPoint =
      " LEFT JOIN review_point rp"
          + " ON link.id = rp.link_id "
          + "   AND rp.user_id = :user"
          + " WHERE "
          + "   rp.id IS NULL ";

  String byAncestorWhereThereIsNoReviewPoint =
      "JOIN notes_closure ON notes_closure.note_id = source_id "
          + "   AND notes_closure.ancestor_id = :ancestor "
          + whereThereIsNoReviewPoint;

  String selectLinkWithLevelFromNotes =
      ", GREATEST(source.level, target.level) as level from link "
          + "INNER JOIN ("
          + " SELECT sourceNote.id, srs.level as level FROM note sourceNote LEFT JOIN review_setting srs ON sourceNote.master_review_setting_id = srs.id) as source "
          + " ON source.id = link.source_id "
          + "INNER JOIN ("
          + " SELECT targetNote.id, trs.level as level FROM note targetNote LEFT JOIN review_setting trs ON targetNote.master_review_setting_id = trs.id) as target "
          + " ON target.id = link.target_id ";

  String byOwnershipWhereThereIsNoReviewPoint =
      " JOIN note ON note.id = source_id"
          + " JOIN notebook ON notebook.id = note.notebook_id "
          + "   AND notebook.ownership_id = :#{#user.ownership.id} "
          + whereThereIsNoReviewPoint;

  String selectThings =
      ", GREATEST(jlink.level, 0) as level from thing "
          + "INNER JOIN ("
          + " SELECT link.id"
          + selectLinkWithLevelFromNotes
          + byOwnershipWhereThereIsNoReviewPoint
          + ") jlink ON jlink.id = thing.link_id ";

  String selectThingsByAncestor =
      ", GREATEST(jlink.level, 0) as level from thing "
          + "INNER JOIN ("
          + " SELECT link.id"
          + selectLinkWithLevelFromNotes
          + byAncestorWhereThereIsNoReviewPoint
          + ") jlink ON jlink.id = thing.link_id ";

  String orderByDate = " ORDER BY level, thing.created_at";

  @Query(value = "SELECT thing.* " + selectNoteThings + orderByDate, nativeQuery = true)
  Stream<Thing> findNotesByOwnershipWhereThereIsNoReviewPoint(@Param("user") User user);

  @Query(value = "SELECT thing.* " + selectNoteThingsByAncestor + orderByDate, nativeQuery = true)
  Stream<Thing> findNotesByAncestorWhereThereIsNoReviewPoint(@Param("user") User user, @Param("ancestor") Note ancestor);

  String whereNoteThereIsNoReviewPoint =
      " LEFT JOIN review_point rp"
          + "   ON note.id = rp.note_id "
          + "     AND rp.user_id = :#{#user.id} "
          + " LEFT JOIN review_setting rs "
          + "   ON note.master_review_setting_id = rs.id "
          + " WHERE note.skip_review IS FALSE "
          + "   AND rp.id IS NULL "
          + "   AND note.deleted_at IS NULL ";

  String byNoteOwnershipWhereThereIsNoReviewPoint =
      " JOIN notebook ON notebook.id = note.notebook_id "
          + " AND notebook.ownership_id = :#{#user.ownership.id} "
          + whereNoteThereIsNoReviewPoint;

  String selectNoteThings =
      ", GREATEST(jnote.level, 0) as level from thing "
          + "INNER JOIN ("
          + " SELECT note.id as id, rs.level as level FROM note"
          + byNoteOwnershipWhereThereIsNoReviewPoint
          + ") jnote ON jnote.id = thing.note_id ";

  String joinClosure =
    " JOIN notes_closure ON notes_closure.note_id = note.id "
      + "   AND notes_closure.ancestor_id = :ancestor ";

  String selectNoteThingsByAncestor =
    ", GREATEST(jnote.level, 0) as level from thing "
      + "INNER JOIN ("
      + " SELECT note.id as id, rs.level as level FROM note"
      + joinClosure + whereNoteThereIsNoReviewPoint
      + ") jnote ON jnote.id = thing.note_id ";

}
