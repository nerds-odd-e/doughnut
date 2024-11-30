package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Note;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface NoteReviewRepository extends CrudRepository<Note, Integer> {
  String selectThingsFrom = "SELECT n  from Note n  ";

  @Query(value = selectThingsFrom + selectNoteThings + joinReviewPoint + whereClaus + orderByDate)
  Stream<Note> findByOwnershipWhereThereIsNoReviewPoint(Integer userId, Integer ownershipId);

  @Query(
      value =
          "SELECT count(1) as count from Note n " + selectNoteThings + joinReviewPoint + whereClaus)
  int countByOwnershipWhereThereIsNoReviewPoint(Integer userId, Integer ownershipId);

  @Query(value = selectThingsFrom + joinReviewPoint + whereClaus + fromNotebook + orderByDate)
  Stream<Note> findByAncestorWhereThereIsNoReviewPoint(Integer userId, Integer notebookId);

  @Query(
      value = "SELECT count(1) as count from Note n " + joinReviewPoint + whereClaus + fromNotebook)
  int countByAncestorWhereThereIsNoReviewPoint(Integer userId, Integer notebookId);

  @Query(value = "SELECT count(1) as count from Note n " + " WHERE n.id in :noteIds" + fromNotebook)
  int countByAncestorAndInTheList(Integer notebookId, @Param("noteIds") List<Integer> noteIds);

  String whereClaus =
      " WHERE "
          + "   rp IS NULL "
          + "   AND COALESCE(n.reviewSetting.skipReview, FALSE) = FALSE "
          + "   AND n.deletedAt IS NULL ";

  String joinReviewPoint = " LEFT JOIN n.memoryTrackers rp ON rp.user.id = :userId";

  String orderByDate = " ORDER BY n.reviewSetting.level, n.createdAt, n.id";

  String fromNotebook = "   AND n.notebook.id = :notebookId ";

  String selectNoteThings = " JOIN n.notebook nb " + " ON nb.ownership.id = :ownershipId ";
}
