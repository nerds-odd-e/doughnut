package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Note;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface NoteReviewRepository extends CrudRepository<Note, Integer> {
  String selectThingsFrom = "SELECT n  from Note n  ";

  @Query(value = selectThingsFrom + selectNoteThings + joinMemoryTracker + whereClaus + orderByDate)
  Stream<Note> findByOwnershipWhereThereIsNoMemoryTracker(Integer userId, Integer ownershipId);

  @Query(
      value =
          "SELECT count(1) as count from Note n "
              + selectNoteThings
              + joinMemoryTracker
              + whereClaus)
  int countByOwnershipWhereThereIsNoMemoryTracker(Integer userId, Integer ownershipId);

  @Query(value = selectThingsFrom + joinMemoryTracker + whereClaus + fromNotebook + orderByDate)
  Stream<Note> findByAncestorWhereThereIsNoMemoryTracker(Integer userId, Integer notebookId);

  @Query(
      value =
          "SELECT count(1) as count from Note n " + joinMemoryTracker + whereClaus + fromNotebook)
  int countByAncestorWhereThereIsNoMemoryTracker(Integer userId, Integer notebookId);

  @Query(value = "SELECT count(1) as count from Note n " + " WHERE n.id in :noteIds" + fromNotebook)
  int countByAncestorAndInTheList(Integer notebookId, @Param("noteIds") List<Integer> noteIds);

  String whereClaus =
      " WHERE "
          + "   rp IS NULL "
          + "   AND COALESCE(n.recallSetting.skipMemoryTracking, FALSE) = FALSE "
          + "   AND n.deletedAt IS NULL ";

  String joinMemoryTracker =
      " LEFT JOIN n.memoryTrackers rp ON rp.user.id = :userId AND rp.deletedAt IS NULL";

  String orderByDate = " ORDER BY n.recallSetting.level, n.createdAt, n.id";

  String fromNotebook = "   AND n.notebook.id = :notebookId ";

  String selectNoteThings = " JOIN n.notebook nb " + " ON nb.ownership.id = :ownershipId ";
}
