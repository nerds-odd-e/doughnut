package com.odde.donut.entities.repositories;

import com.odde.donut.entities.AssimilationSequenceSkip;
import com.odde.donut.entities.MemoryTrackerQueryFragments;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.NoteLevelIndex;
import com.odde.donut.entities.NotebookSettings;
import com.odde.donut.services.AssimilationUnit;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Queries that select notes for assimilation (unassimilated units and counts). */
public interface NoteAssimilationQueries {

  String unassimilatedWhereClause =
      " WHERE "
          + "   rp IS NULL "
          + "   AND "
          + Note.JPA_AVAILABLE
          + " "
          + "   AND "
          + AssimilationSequenceSkip.JPA_NOT_EXISTS_NOTE_LEVEL_SKIP
          + " AND "
          + NotebookSettings.JPA_NOTEBOOK_NOT_SKIP_MEMORY_TRACKING;

  String joinMemoryTracker =
      " LEFT JOIN n.memoryTrackers rp ON rp.user.id = :userId"
          + " AND "
          + MemoryTrackerQueryFragments.JPA_WHERE_NOTE_LEVEL_TRACKER
          + " AND "
          + MemoryTrackerQueryFragments.JPA_WHERE_UNDERSTANDING_TRACKER;

  String unassimilatedOrderBy = " ORDER BY " + NoteLevelIndex.JPA_LEVEL + ", n.createdAt, n.id";

  String selectUnassimilatedNoteUnit =
      "SELECT NEW com.odde.donut.services.AssimilationUnit(n, "
          + NoteLevelIndex.JPA_LEVEL
          + ") FROM Note n";

  String selectFromNoteWithOwnership =
      " JOIN n.notebook nb " + " ON nb.ownership.id = :ownershipId ";

  String fromNotebook = "   AND n.notebook.id = :notebookId ";

  @Query(
      value =
          selectUnassimilatedNoteUnit
              + selectFromNoteWithOwnership
              + joinMemoryTracker
              + NoteLevelIndex.JPA_LEFT_JOIN
              + unassimilatedWhereClause
              + unassimilatedOrderBy)
  Stream<AssimilationUnit> findUnassimilatedByOwnership(Integer userId, Integer ownershipId);

  @Query(
      value =
          "SELECT count(1) as count from Note n "
              + selectFromNoteWithOwnership
              + joinMemoryTracker
              + unassimilatedWhereClause)
  int countUnassimilatedByOwnership(Integer userId, Integer ownershipId);

  @Query(
      value =
          selectUnassimilatedNoteUnit
              + joinMemoryTracker
              + NoteLevelIndex.JPA_LEFT_JOIN
              + unassimilatedWhereClause
              + fromNotebook
              + unassimilatedOrderBy)
  Stream<AssimilationUnit> findUnassimilatedByAncestor(Integer userId, Integer notebookId);

  @Query(
      value =
          "SELECT count(1) as count from Note n "
              + joinMemoryTracker
              + unassimilatedWhereClause
              + fromNotebook)
  int countUnassimilatedByAncestor(Integer userId, Integer notebookId);

  @Query(value = "SELECT count(1) as count from Note n " + " WHERE n.id in :noteIds" + fromNotebook)
  int countByAncestorAndInTheList(Integer notebookId, @Param("noteIds") List<Integer> noteIds);
}
