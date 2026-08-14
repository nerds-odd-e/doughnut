package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.AssimilationSequenceSkip;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.NotePropertyIndex;
import com.odde.doughnut.entities.NotebookSettings;
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
          + " AND mt.type <> com.odde.doughnut.entities.MemoryTrackerType.SPELLING"
          + " AND mt.propertyKey = i.propertyKey";

  String targetNoteKeyGateWhere =
      " AND NOT EXISTS ("
          + " SELECT iBlock FROM NotePropertyIndex iBlock"
          + " JOIN iBlock.targetNote tBlock"
          + " LEFT JOIN tBlock.memoryTrackers tmtBlock ON tmtBlock.user.id = :userId"
          + " AND tmtBlock.deletedAt IS NULL"
          + " AND "
          + MemoryTracker.JPA_WHERE_NOTE_LEVEL_TARGET_TRACKER
          + " AND "
          + MemoryTracker.JPA_WHERE_UNDERSTANDING_TARGET_TRACKER
          + " WHERE iBlock.note = n AND iBlock.propertyKey = i.propertyKey"
          + " AND tBlock.deletedAt IS NULL"
          + " AND tmtBlock IS NULL"
          + ") ";

  String unassimilatedWhereClause =
      " WHERE mt IS NULL"
          + " AND n.deletedAt IS NULL "
          + " AND "
          + AssimilationSequenceSkip.JPA_NOT_EXISTS_PROPERTY_SKIP
          + " AND "
          + NotebookSettings.JPA_NOTEBOOK_NOT_SKIP_MEMORY_TRACKING;

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
              + unassimilatedWhereClause
              + targetNoteKeyGateWhere
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
              + unassimilatedWhereClause
              + targetNoteKeyGateWhere
              + " AND nb.id = :notebookId"
              + unassimilatedDedupeByExactKey
              + unassimilatedOrderBy)
  Stream<NotePropertyIndex> streamUnassimilatedPropertiesForNotebook(
      @Param("userId") Integer userId, @Param("notebookId") Integer notebookId);
}
