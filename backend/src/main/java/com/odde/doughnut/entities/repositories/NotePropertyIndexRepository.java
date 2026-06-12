package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.NotePropertyIndex;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotePropertyIndexRepository extends JpaRepository<NotePropertyIndex, Integer> {

  String unassimilatedJoinMemoryTracker =
      " LEFT JOIN n.memoryTrackers mt ON mt.user.id = :userId"
          + " AND mt.deletedAt IS NULL"
          + " AND COALESCE(mt.spelling, FALSE) = FALSE"
          + " AND mt.propertyKey = i.propertyKey";

  String unassimilatedWhereClause =
      " WHERE mt IS NULL"
          + " AND COALESCE(n.recallSetting.skipMemoryTracking, FALSE) = FALSE"
          + " AND n.deletedAt IS NULL ";

  String unassimilatedOrderBy = " ORDER BY n.recallSetting.level, n.createdAt, n.id, i.propertyKey";

  @Modifying
  @Query("DELETE FROM NotePropertyIndex i WHERE i.note.id = :noteId")
  void deleteByNoteIdInBulk(@Param("noteId") Integer noteId);

  List<NotePropertyIndex> findByNote_IdOrderByIdAsc(Integer noteId);

  @Query(
      value =
          "SELECT COUNT(i) FROM NotePropertyIndex i"
              + " JOIN i.note n"
              + " JOIN n.notebook nb ON nb.ownership.id = :ownershipId"
              + unassimilatedJoinMemoryTracker
              + unassimilatedWhereClause)
  int countUnassimilatedPropertiesForOwnership(
      @Param("userId") Integer userId, @Param("ownershipId") Integer ownershipId);

  @Query(
      value =
          "SELECT COUNT(i) FROM NotePropertyIndex i"
              + " JOIN i.note n"
              + " JOIN n.notebook nb"
              + unassimilatedJoinMemoryTracker
              + unassimilatedWhereClause
              + " AND nb.id = :notebookId")
  int countUnassimilatedPropertiesForNotebook(
      @Param("userId") Integer userId, @Param("notebookId") Integer notebookId);

  @Query(
      value =
          "SELECT i FROM NotePropertyIndex i"
              + " JOIN FETCH i.note n"
              + " JOIN n.notebook nb ON nb.ownership.id = :ownershipId"
              + unassimilatedJoinMemoryTracker
              + unassimilatedWhereClause
              + unassimilatedOrderBy)
  Stream<NotePropertyIndex> streamUnassimilatedPropertiesForOwnership(
      @Param("userId") Integer userId, @Param("ownershipId") Integer ownershipId);

  @Query(
      value =
          "SELECT i FROM NotePropertyIndex i"
              + " JOIN FETCH i.note n"
              + " JOIN n.notebook nb"
              + unassimilatedJoinMemoryTracker
              + unassimilatedWhereClause
              + " AND nb.id = :notebookId"
              + unassimilatedOrderBy)
  Stream<NotePropertyIndex> streamUnassimilatedPropertiesForNotebook(
      @Param("userId") Integer userId, @Param("notebookId") Integer notebookId);
}
