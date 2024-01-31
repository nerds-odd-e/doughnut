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

  String selectThingsFrom = "SELECT thing.*,  jnote.level as level from thing  ";

  @Query(
      value = selectThingsFrom + selectNoteThings + orderByDate,
      nativeQuery = true)
  Stream<Thing> findByOwnershipWhereThereIsNoReviewPoint(Integer userId, Integer ownershipId);

  @Query(
      value =
          "SELECT count(1) as count from thing "
              + selectNoteThings
              + whereThereIsNoReviewPoint,
      nativeQuery = true)
  int countByOwnershipWhereThereIsNoReviewPoint(Integer userId, Integer ownershipId);

  @Query(
      value = selectThingsFrom + selectNoteThingFromNotebook + orderByDate,
      nativeQuery = true)
  Stream<Thing> findByAncestorWhereThereIsNoReviewPoint(Integer userId, Integer notebookId);

  @Query(
      value =
          "SELECT count(1) as count from thing "
              + selectNoteThingFromNotebook
              + whereThereIsNoReviewPoint,
      nativeQuery = true)
  int countByAncestorWhereThereIsNoReviewPoint(Integer userId, Integer notebookId);

  @Query(
      value =
          "SELECT count(1) as count from thing "
              + selectNoteThingFromNotebook
              + " WHERE (jnote.id IS NOT NULL) AND thing.id in :thingIds",
      nativeQuery = true)
  int countByAncestorAndInTheList(Integer notebookId, @Param("thingIds") List<Integer> thingIds);

  String joinNotebook =
      " JOIN notebook ON notebook.id = note.notebook_id "
          + " AND notebook.ownership_id = :ownershipId ";

  String whereThereIsNoReviewPoint =
      " LEFT JOIN review_point rp"
          + " ON thing.id = rp.thing_id "
          + "   AND rp.user_id = :userId"
          + " WHERE "
          + "   rp.id IS NULL "
          + "   AND (jnote.id IS NOT NULL) ";

  String orderByDate = whereThereIsNoReviewPoint + " ORDER BY level, thing.created_at, id";

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

  String selectNoteThings = selectThingJoinNote + joinNotebook + whereNoteIsNotSkipped;
  String selectNoteThingFromNotebook = selectThingJoinNote + fromNotebook + whereNoteIsNotSkipped;
}
