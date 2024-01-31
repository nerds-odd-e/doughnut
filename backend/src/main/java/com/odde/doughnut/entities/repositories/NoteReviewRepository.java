package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Note;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface NoteReviewRepository extends CrudRepository<Note, Integer> {
  String selectThingsFrom = "SELECT nx.*  from note nx  ";

  @Query(
      value = selectThingsFrom + selectNoteThings + joinReviewPoint + whereClaus + orderByDate,
      nativeQuery = true)
  Stream<Note> findByOwnershipWhereThereIsNoReviewPoint(Integer userId, Integer ownershipId);

  @Query(
      value =
          "SELECT count(1) as count from note nx "
              + selectNoteThings
              + joinReviewPoint
              + whereClaus,
      nativeQuery = true)
  int countByOwnershipWhereThereIsNoReviewPoint(Integer userId, Integer ownershipId);

  @Query(
      value =
          selectThingsFrom
              + selectThingJoinNote
              + joinReviewPoint
              + whereClaus
              + fromNotebook
              + orderByDate,
      nativeQuery = true)
  Stream<Note> findByAncestorWhereThereIsNoReviewPoint(Integer userId, Integer notebookId);

  @Query(
      value =
          "SELECT count(1) as count from note nx "
              + selectThingJoinNote
              + joinReviewPoint
              + whereClaus
              + fromNotebook,
      nativeQuery = true)
  int countByAncestorWhereThereIsNoReviewPoint(Integer userId, Integer notebookId);

  @Query(
      value =
          "SELECT count(1) as count from note nx "
              + selectThingJoinNote
              + " WHERE nx.id in :noteIds"
              + fromNotebook,
      nativeQuery = true)
  int countByAncestorAndInTheList(Integer notebookId, @Param("noteIds") List<Integer> noteIds);

  String whereClaus =
      " WHERE "
          + "   rp.id IS NULL "
          + "   AND nx.skip_review IS NOT TRUE "
          + "   AND nx.deleted_at IS NULL ";

  String joinReviewPoint =
      " LEFT JOIN review_point rp" + " ON nx.id = rp.note_id " + "   AND rp.user_id = :userId";

  String orderByDate = " ORDER BY level, created_at, id";

  String fromNotebook = "   AND nx.notebook_id = :notebookId ";

  String selectThingJoinNote = " ";

  String selectNoteThings =
      selectThingJoinNote
          + " JOIN notebook ON notebook.id = nx.notebook_id "
          + " AND notebook.ownership_id = :ownershipId ";
}
