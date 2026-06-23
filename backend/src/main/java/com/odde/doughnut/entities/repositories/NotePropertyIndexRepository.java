package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.NotePropertyIndex;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotePropertyIndexRepository extends JpaRepository<NotePropertyIndex, Integer> {

  String unassimilatedJoinPropertyTracker =
      " LEFT JOIN n.memoryTrackers mt ON mt.user.id = :userId"
          + " AND mt.deletedAt IS NULL"
          + " AND COALESCE(mt.spelling, FALSE) = FALSE"
          + " AND mt.propertyKey = i.propertyKey";

  String targetNoteGateJoin =
      " LEFT JOIN i.targetNote t"
          + " LEFT JOIN t.memoryTrackers tmt ON tmt.user.id = :userId"
          + " AND tmt.deletedAt IS NULL"
          + " AND "
          + MemoryTracker.JPA_WHERE_NOTE_LEVEL_TARGET_TRACKER;

  String unassimilatedWhereClause =
      " WHERE mt IS NULL"
          + " AND COALESCE(n.recallSetting.skipMemoryTracking, FALSE) = FALSE"
          + " AND n.deletedAt IS NULL ";

  String targetNoteGateWhere =
      " AND (t IS NULL OR t.deletedAt IS NOT NULL"
          + " OR COALESCE(t.recallSetting.skipMemoryTracking, FALSE) = TRUE"
          + " OR tmt IS NOT NULL) ";

  String unassimilatedDedupeByExactKey =
      " AND i.itemIndex = (SELECT MIN(i2.itemIndex) FROM NotePropertyIndex i2"
          + " WHERE i2.note = n AND i2.propertyKey = i.propertyKey)";

  String unassimilatedOrderBy = " ORDER BY n.recallSetting.level, n.createdAt, n.id, i.propertyKey";

  @Modifying
  @Query("DELETE FROM NotePropertyIndex i WHERE i.note.id = :noteId")
  void deleteByNoteIdInBulk(@Param("noteId") Integer noteId);

  List<NotePropertyIndex> findByNote_IdOrderByIdAsc(Integer noteId);

  @Query(
      value =
          "SELECT i FROM NotePropertyIndex i"
              + " JOIN FETCH i.note n"
              + " JOIN n.notebook nb ON nb.ownership.id = :ownershipId"
              + unassimilatedJoinPropertyTracker
              + targetNoteGateJoin
              + unassimilatedWhereClause
              + targetNoteGateWhere
              + unassimilatedDedupeByExactKey
              + unassimilatedOrderBy)
  Stream<NotePropertyIndex> streamUnassimilatedPropertiesForOwnership(
      @Param("userId") Integer userId, @Param("ownershipId") Integer ownershipId);

  @Query(
      value =
          "SELECT i FROM NotePropertyIndex i"
              + " JOIN FETCH i.note n"
              + " JOIN n.notebook nb"
              + unassimilatedJoinPropertyTracker
              + targetNoteGateJoin
              + unassimilatedWhereClause
              + targetNoteGateWhere
              + " AND nb.id = :notebookId"
              + unassimilatedDedupeByExactKey
              + unassimilatedOrderBy)
  Stream<NotePropertyIndex> streamUnassimilatedPropertiesForNotebook(
      @Param("userId") Integer userId, @Param("notebookId") Integer notebookId);
}
