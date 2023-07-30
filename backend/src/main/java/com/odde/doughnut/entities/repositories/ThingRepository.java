package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Thing;
import com.odde.doughnut.entities.User;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface ThingRepository extends CrudRepository<Thing, Integer> {
  @Query(value = "SELECT thing.* FROM thing where id in (:ids)", nativeQuery = true)
  Stream<Thing> findAllByIds(List<Integer> ids);

  String selectThingsFrom =
      "SELECT thing.*,  IFNULL(IFNULL(jlink.level, jnote.level),0) as level from thing  ";

  @Query(
      value = selectThingsFrom + selectThings + selectNoteThings + orderByDate,
      nativeQuery = true)
  Stream<Thing> findByOwnershipWhereThereIsNoReviewPoint(@Param("user") User user);

  @Query(
      value =
          "SELECT count(1) as count from thing "
              + selectThings
              + selectNoteThings
              + whereThereIsNoReviewPoint,
      nativeQuery = true)
  int countByOwnershipWhereThereIsNoReviewPoint(@Param("user") User user);

  @Query(
      value = selectThingsFrom + selectThingsByAncestor + selectNoteThingsByAncestor + orderByDate,
      nativeQuery = true)
  Stream<Thing> findByAncestorWhereThereIsNoReviewPoint(
      @Param("user") User user, @Param("ancestor") Note ancestor);

  @Query(
      value =
          "SELECT count(1) as count from thing "
              + selectThingsByAncestor
              + selectNoteThingsByAncestor
              + whereThereIsNoReviewPoint,
      nativeQuery = true)
  int countByAncestorWhereThereIsNoReviewPoint(
      @Param("user") User user, @Param("ancestor") Note ancestor);

  @Query(
      value =
          "SELECT count(1) as count from thing "
              + selectThingsByAncestor
              + selectNoteThingsByAncestor
              + " WHERE (jlink.id IS NOT NULL OR jnote.id IS NOT NULL) AND thing.id in :thingIds",
      nativeQuery = true)
  int countByAncestorAndInTheList(
      @Param("ancestor") Note ancestor, @Param("thingIds") List<Integer> thingIds);

  String byAncestorWhereThereIsNoReviewPoint =
      "JOIN notes_closure ON notes_closure.note_id = source_id "
          + "   AND notes_closure.ancestor_id = :ancestor ";

  String selectLinkWithLevelFromNotes =
      ", GREATEST(IFNULL(source.level, 0), IFNULL(target.level, 0)) as level from link "
          + "INNER JOIN ("
          + " SELECT sourceNote.id, srs.level as level FROM note sourceNote LEFT JOIN review_setting srs ON sourceNote.master_review_setting_id = srs.id) as source "
          + " ON source.id = link.source_id "
          + "INNER JOIN ("
          + " SELECT targetNote.id, trs.level as level FROM note targetNote LEFT JOIN review_setting trs ON targetNote.master_review_setting_id = trs.id) as target "
          + " ON target.id = link.target_id ";

  String joinNotebook =
      " JOIN notebook ON notebook.id = note.notebook_id "
          + " AND notebook.ownership_id = :#{#user.ownership.id} ";

  String byOwnershipWhereThereIsNoReviewPoint = " JOIN note ON note.id = source_id" + joinNotebook;

  String selectThingsJoinLink = "LEFT JOIN (" + " SELECT link.id" + selectLinkWithLevelFromNotes;

  String selectThingsByAncestor =
      selectThingsJoinLink
          + byAncestorWhereThereIsNoReviewPoint
          + ") jlink ON jlink.id = thing.link_id ";

  String whereThereIsNoReviewPoint =
      " LEFT JOIN review_point rp"
          + " ON thing.id = rp.thing_id "
          + "   AND rp.user_id = :user"
          + " WHERE "
          + "   rp.id IS NULL "
          + "   AND (jlink.id IS NOT NULL OR jnote.id IS NOT NULL) ";

  String orderByDate = whereThereIsNoReviewPoint + " ORDER BY level, thing.created_at";

  String whereNoteIsNotSkipped =
      " LEFT JOIN review_setting rs "
          + "   ON note.master_review_setting_id = rs.id "
          + " WHERE rs.skip_review IS FALSE "
          + "   AND note.deleted_at IS NULL "
          + ") jnote ON jnote.id = thing.note_id ";

  String joinClosure =
      " JOIN notes_closure ON notes_closure.note_id = note.id "
          + "   AND notes_closure.ancestor_id = :ancestor ";

  String selectThingJoinNote = "LEFT JOIN (" + " SELECT note.id as id, rs.level as level FROM note";

  String selectThings =
      selectThingsJoinLink
          + byOwnershipWhereThereIsNoReviewPoint
          + ") jlink ON jlink.id = thing.link_id ";

  String selectNoteThings = selectThingJoinNote + joinNotebook + whereNoteIsNotSkipped;

  String selectNoteThingsByAncestor = selectThingJoinNote + joinClosure + whereNoteIsNotSkipped;
}
