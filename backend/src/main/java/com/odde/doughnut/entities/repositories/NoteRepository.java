package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Note;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface NoteRepository extends CrudRepository<Note, Integer> {

  long countByNotebook_Id(Integer notebookId);

  String selectFromNote = "SELECT n FROM Note n";
  String searchForTitleLike = " WHERE n.title LIKE :pattern AND n.deletedAt IS NULL ";
  String searchForTitleExact = " WHERE LOWER(n.title) = LOWER(:key) AND n.deletedAt IS NULL ";

  @Query(value = selectFromNote + " where n.title = :key")
  Note findFirstByTitle(@Param("key") String key);

  @Query(
      value =
          selectFromNote
              + " WHERE n.notebook.id IN ("
              + "SELECT nb.id FROM Notebook nb WHERE nb.name = :notebookName AND nb.deletedAt IS NULL)"
              + " AND n.title = :key")
  Note findFirstInNotebookByName(
      @Param("notebookName") String notebookName, @Param("key") String key);

  @Query(
      value =
          selectFromNote
              + " JOIN FETCH n.notebook nb "
              + " WHERE n.title = :noteTitle AND n.deletedAt IS NULL "
              + " AND nb.deletedAt IS NULL "
              + " AND nb.name = :notebookName "
              + " ORDER BY n.id ASC")
  List<Note> findByNotebookNameAndNoteTitleOrderByIdAsc(
      @Param("notebookName") String notebookName, @Param("noteTitle") String noteTitle);

  @Query(
      value = selectFromNote + searchForTitleLike + "  AND n.notebook.ownership.user.id = :userId")
  List<Note> searchForUserInAllMyNotebooks(Integer userId, String pattern, Pageable pageable);

  @Query(
      value = selectFromNote + searchForTitleExact + "  AND n.notebook.ownership.user.id = :userId")
  List<Note> searchExactForUserInAllMyNotebooks(Integer userId, String key);

  @Query(
      value =
          selectFromNote
              + " JOIN n.notebook.subscriptions s ON s.user.id = :userId "
              + searchForTitleExact)
  List<Note> searchExactForUserInAllMySubscriptions(Integer userId, @Param("key") String key);

  @Query(
      value =
          selectFromNote
              + "              JOIN n.notebook.ownership.circle.members m"
              + "                ON m.id = :userId "
              + searchForTitleExact)
  List<Note> searchExactForUserInAllMyCircle(Integer userId, @Param("key") String key);

  @Query(value = selectFromNote + searchForTitleExact + " AND n.notebook.id = :notebookId")
  List<Note> searchExactInNotebook(Integer notebookId, @Param("key") String key);

  @Query(
      value =
          selectFromNote
              + " WHERE n.notebook.id = :notebookId AND n.deletedAt IS NULL AND n.folder IS NULL "
              + " AND LOWER(n.title) = 'index' ORDER BY n.id ASC")
  List<Note> findRootIndexNoteCandidatesForNotebook(
      @Param("notebookId") Integer notebookId, Pageable pageable);

  @Query(
      value =
          selectFromNote
              + " WHERE n.notebook.id = :notebookId AND n.deletedAt IS NULL AND n.parent IS NULL"
              + " ORDER BY n.id ASC")
  List<Note> findNotebookRootNotesByNotebookId(@Param("notebookId") Integer notebookId);

  @Query(
      value =
          selectFromNote
              + " WHERE n.notebook.id = :notebookId AND n.deletedAt IS NULL AND n.folder IS NULL"
              + " ORDER BY n.siblingOrder ASC, n.id ASC")
  List<Note> findNotesInNotebookRootFolderScopeByNotebookId(
      @Param("notebookId") Integer notebookId);

  @Query(
      value =
          selectFromNote
              + " WHERE n.folder.id = :folderId AND n.deletedAt IS NULL"
              + " ORDER BY n.siblingOrder ASC, n.id ASC")
  List<Note> findNotesInFolderOrderBySiblingOrder(@Param("folderId") Integer folderId);

  @Query(
      value =
          selectFromNote
              + " JOIN n.notebook.subscriptions s ON s.user.id = :userId "
              + searchForTitleLike)
  List<Note> searchForUserInAllMySubscriptions(
      Integer userId, @Param("pattern") String pattern, Pageable pageable);

  @Query(
      value =
          selectFromNote
              + "              JOIN n.notebook.ownership.circle.members m"
              + "                ON m.id = :userId "
              + searchForTitleLike)
  List<Note> searchForUserInAllMyCircle(
      Integer userId, @Param("pattern") String pattern, Pageable pageable);

  @Query(value = selectFromNote + searchForTitleLike + " AND n.notebook.id = :notebookId")
  List<Note> searchInNotebook(
      Integer notebookId, @Param("pattern") String pattern, Pageable pageable);

  @Query(
      value =
          selectFromNote
              + " WHERE n.notebook.id = :notebookId "
              + " AND n.wikidataId = :wikidataId AND n.wikidataId IS NOT NULL AND n.deletedAt IS NULL ")
  List<Note> noteWithWikidataIdWithinNotebook(
      @Param("notebookId") Integer notebookId, @Param("wikidataId") String wikidataId);

  @Query(
      value =
          selectFromNote
              + " WHERE n.notebook.ownership.user.id = :userId"
              + " AND n.deletedAt IS NULL"
              + " ORDER BY n.updatedAt DESC"
              + " LIMIT 20")
  List<Note> findRecentNotesByUser(@Param("userId") Integer userId);

  @Query(value = selectFromNote + " WHERE n.targetNote.id = :targetNoteId")
  List<Note> findAllByTargetNote(@Param("targetNoteId") Integer targetNoteId);

  @Query(value = selectFromNote + " WHERE n.parent.id = :parentId")
  List<Note> findAllByParentId(@Param("parentId") Integer parentId);

  String RELATIONSHIP_WIKI_MIGRATION_ELIGIBILITY =
      " n.targetNote IS NOT NULL "
          + "AND n.deletedAt IS NULL "
          + "AND n.relationType IS NOT NULL "
          + "AND n.parent IS NOT NULL "
          + "AND (n.title IS NULL OR TRIM(n.title) = '' "
          + "OR n.details IS NULL OR TRIM(n.details) = '' "
          + "OR n.details NOT LIKE '%type: relationship%' "
          + "OR NOT EXISTS (SELECT 1 FROM NoteWikiTitleCache c WHERE c.note = n)) ";

  @Query("SELECT COUNT(n) FROM Note n WHERE " + RELATIONSHIP_WIKI_MIGRATION_ELIGIBILITY)
  long countRelationshipNotesEligibleForWikiReferenceMigration();

  @Query(
      "SELECT n.id FROM Note n WHERE "
          + RELATIONSHIP_WIKI_MIGRATION_ELIGIBILITY
          + "AND (:exclusiveAfter IS NULL OR n.id > :exclusiveAfter) "
          + "ORDER BY n.id ASC")
  List<Integer> findRelationshipWikiMigrationCandidateIdsExclusiveAfterAsc(
      @Param("exclusiveAfter") Integer exclusiveAfter, Pageable pageable);

  @Query(
      "SELECT DISTINCT n FROM Note n "
          + "JOIN FETCH n.parent JOIN FETCH n.targetNote JOIN FETCH n.notebook "
          + "WHERE n.id IN :ids")
  List<Note> hydrateRelationshipWikiMigrationNotesByIds(@Param("ids") List<Integer> ids);

  @Query("SELECT COUNT(n) FROM Note n WHERE n.deletedAt IS NULL")
  long countNonDeletedNotes();

  @Query(
      "SELECT n.id FROM Note n WHERE n.deletedAt IS NULL "
          + "AND (:exclusiveAfter IS NULL OR n.id > :exclusiveAfter) "
          + "ORDER BY n.id ASC")
  List<Integer> findNonDeletedNoteIdsExclusiveAfterAsc(
      @Param("exclusiveAfter") Integer exclusiveAfter, Pageable pageable);

  @Query(
      value =
          "SELECT DISTINCT n FROM Note n JOIN FETCH n.notebook LEFT JOIN FETCH n.folder "
              + "WHERE n.id IN :ids")
  List<Note> hydrateNonDeletedNotesWithNotebookAndFolderByIds(@Param("ids") List<Integer> ids);

  @Query(
      value =
          "SELECT DISTINCT n FROM Note n JOIN FETCH n.notebook LEFT JOIN FETCH n.folder "
              + "WHERE n.deletedAt IS NULL "
              + "ORDER BY n.notebook.id ASC, n.folder.id ASC NULLS LAST, n.id ASC")
  List<Note> findAllNonDeletedNotesOrderByNotebookFolderAndId();

  Optional<Note> findFirstByParent_IdAndFolderIsNotNullAndDeletedAtIsNullOrderByIdAsc(
      Integer parentId);

  String recallWhereClause =
      " WHERE "
          + "   rp IS NULL "
          + "   AND COALESCE(n.recallSetting.skipMemoryTracking, FALSE) = FALSE "
          + "   AND n.deletedAt IS NULL ";

  String joinMemoryTracker =
      " LEFT JOIN n.memoryTrackers rp ON rp.user.id = :userId AND rp.deletedAt IS NULL";

  String recallOrderByDate = " ORDER BY n.recallSetting.level, n.createdAt, n.id";

  String selectFromNoteWithOwnership =
      " JOIN n.notebook nb " + " ON nb.ownership.id = :ownershipId ";

  @Query(
      value =
          selectFromNote
              + selectFromNoteWithOwnership
              + joinMemoryTracker
              + recallWhereClause
              + recallOrderByDate)
  Stream<Note> findByOwnershipWhereThereIsNoMemoryTracker(Integer userId, Integer ownershipId);

  @Query(
      value =
          "SELECT count(1) as count from Note n "
              + selectFromNoteWithOwnership
              + joinMemoryTracker
              + recallWhereClause)
  int countByOwnershipWhereThereIsNoMemoryTracker(Integer userId, Integer ownershipId);

  String fromNotebook = "   AND n.notebook.id = :notebookId ";

  @Query(
      value =
          selectFromNote + joinMemoryTracker + recallWhereClause + fromNotebook + recallOrderByDate)
  Stream<Note> findByAncestorWhereThereIsNoMemoryTracker(Integer userId, Integer notebookId);

  @Query(
      value =
          "SELECT count(1) as count from Note n "
              + joinMemoryTracker
              + recallWhereClause
              + fromNotebook)
  int countByAncestorWhereThereIsNoMemoryTracker(Integer userId, Integer notebookId);

  @Query(value = "SELECT count(1) as count from Note n " + " WHERE n.id in :noteIds" + fromNotebook)
  int countByAncestorAndInTheList(Integer notebookId, @Param("noteIds") List<Integer> noteIds);

  @Query(value = "SELECT COUNT(n) FROM Note n WHERE n.creator.id = :userId AND n.deletedAt IS NULL")
  long countByCreator(@Param("userId") Integer userId);

  @Query(
      value =
          "SELECT MAX(n.createdAt) FROM Note n WHERE n.creator.id = :userId AND n.deletedAt IS NULL")
  java.sql.Timestamp findLastNoteTimeByCreator(@Param("userId") Integer userId);
}
