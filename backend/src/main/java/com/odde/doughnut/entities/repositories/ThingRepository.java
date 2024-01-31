package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Thing;
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
  Stream<Thing> findByOwnershipWhereThereIsNoReviewPoint(Integer userId, Integer ownershipId);

  @Query(
      value =
          "SELECT count(1) as count from thing "
              + selectThings
              + selectNoteThings
              + whereThereIsNoReviewPoint,
      nativeQuery = true)
  int countByOwnershipWhereThereIsNoReviewPoint(Integer userId, Integer ownershipId);

  @Query(
      value =
          selectThingsFrom + selectThingsFromNotebook + selectNoteThingFromNotebook + orderByDate,
      nativeQuery = true)
  Stream<Thing> findByAncestorWhereThereIsNoReviewPoint(Integer userId, Integer notebookId);

  @Query(
      value =
          "SELECT count(1) as count from thing "
              + selectThingsFromNotebook
              + selectNoteThingFromNotebook
              + whereThereIsNoReviewPoint,
      nativeQuery = true)
  int countByAncestorWhereThereIsNoReviewPoint(Integer userId, Integer notebookId);

  @Query(
      value =
          "SELECT count(1) as count from thing "
              + selectThingsFromNotebook
              + selectNoteThingFromNotebook
              + " WHERE (jlink.id IS NOT NULL OR jnote.id IS NOT NULL) AND thing.id in :thingIds",
      nativeQuery = true)
  int countByAncestorAndInTheList(Integer notebookId, @Param("thingIds") List<Integer> thingIds);

  String byAncestorWhereThereIsNoReviewPoint1 =
      "JOIN note ns ON ns.id = source_id " + "   AND ns.notebook_id = :notebookId ";

  String selectLinkWithLevelFromNotes =
      ", GREATEST(IFNULL(source.level, 0), IFNULL(target.level, 0)) as level from link "
          + "INNER JOIN ("
          + " SELECT sourceNote.id, sourceNote.level as level FROM note sourceNote) as source "
          + " ON source.id = link.source_id "
          + "INNER JOIN ("
          + " SELECT targetNote.id, targetNote.level as level FROM note targetNote) as target "
          + " ON target.id = link.target_id ";

  String joinNotebook =
      " JOIN notebook ON notebook.id = note.notebook_id "
          + " AND notebook.ownership_id = :ownershipId ";

  String byOwnershipWhereThereIsNoReviewPoint = " JOIN note ON note.id = source_id" + joinNotebook;

  String selectThingsJoinLink = "LEFT JOIN (" + " SELECT link.id" + selectLinkWithLevelFromNotes;

  String selectThingsFromNotebook =
      selectThingsJoinLink
          + byAncestorWhereThereIsNoReviewPoint1
          + ") jlink ON jlink.id = thing.link_id ";

  String whereThereIsNoReviewPoint =
      " LEFT JOIN review_point rp"
          + " ON thing.id = rp.thing_id "
          + "   AND rp.user_id = :userId"
          + " WHERE "
          + "   rp.id IS NULL "
          + "   AND (jlink.id IS NOT NULL OR jnote.id IS NOT NULL) ";

  String orderByDate = whereThereIsNoReviewPoint + " ORDER BY level, thing.created_at";

  String whereNoteIsNotSkipped =
      " LEFT JOIN note rs "
          + "   ON note.id = rs.id "
          + " WHERE rs.skip_review IS NOT TRUE "
          + "   AND note.deleted_at IS NULL "
          + ") jnote ON jnote.id = thing.note_id ";

  String fromNotebook =
      " JOIN note ns ON ns.id = note.id " + "   AND ns.notebook_id = :notebookId ";

  String selectThingJoinNote =
      "LEFT JOIN (" + " SELECT note.id as id, note.level as level FROM note";

  String selectThings =
      selectThingsJoinLink
          + byOwnershipWhereThereIsNoReviewPoint
          + ") jlink ON jlink.id = thing.link_id ";

  String selectNoteThings = selectThingJoinNote + joinNotebook + whereNoteIsNotSkipped;

  String selectNoteThingFromNotebook = selectThingJoinNote + fromNotebook + whereNoteIsNotSkipped;
}
