package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.utils.SearchTitleNormalizer;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface NoteRepository extends CrudRepository<Note, Integer> {

  String selectFromNote = "SELECT n FROM Note n";
  String searchForTitleLike =
      " WHERE LOWER("
          + SearchTitleNormalizer.NORMALIZED_NOTE_TITLE_JPQL
          + ") LIKE LOWER(:pattern) AND n.deletedAt IS NULL ";
  String searchForTitleExact =
      " WHERE LOWER("
          + SearchTitleNormalizer.NORMALIZED_NOTE_TITLE_JPQL
          + ") = LOWER(:key) AND n.deletedAt IS NULL ";

  @Query(
      value =
          selectFromNote
              + " WHERE n.notebook.id IN ("
              + "SELECT nb.id FROM Notebook nb "
              + "WHERE LOWER(nb.name) = LOWER(:notebookName) AND nb.deletedAt IS NULL)"
              + " AND LOWER(n.title) = LOWER(:key)")
  Note findFirstInNotebookByName(
      @Param("notebookName") String notebookName, @Param("key") String key);

  @Query(
      value =
          selectFromNote
              + " JOIN FETCH n.notebook nb "
              + " WHERE LOWER(n.title) = LOWER(:noteTitle) AND n.deletedAt IS NULL "
              + " AND nb.deletedAt IS NULL "
              + " AND LOWER(nb.name) = LOWER(:notebookName) "
              + " ORDER BY n.id ASC")
  List<Note> findByNotebookNameAndNoteTitleOrderByIdAsc(
      @Param("notebookName") String notebookName, @Param("noteTitle") String noteTitle);

  @Query(
      value =
          selectFromNote
              + " JOIN FETCH n.notebook nb "
              + " WHERE LOWER(n.title) = LOWER(:noteTitle) AND n.deletedAt IS NULL "
              + " AND nb.deletedAt IS NULL "
              + " ORDER BY n.id ASC")
  List<Note> findByNoteTitleOrderByIdAsc(@Param("noteTitle") String noteTitle);

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
              + " WHERE n.notebook.id = :notebookId AND n.deletedAt IS NULL AND n.folder IS NULL"
              + " ORDER BY n.id ASC")
  List<Note> findNotesInNotebookRootFolderScopeByNotebookId(
      @Param("notebookId") Integer notebookId);

  @Query(
      value =
          selectFromNote
              + " WHERE n.folder.id = :folderId AND n.deletedAt IS NULL"
              + " ORDER BY n.id ASC")
  List<Note> findNotesInFolderOrderByIdAsc(@Param("folderId") Integer folderId);

  @Query(
      """
      SELECT DISTINCT n.folder.id FROM Note n
      WHERE n.notebook.id = :notebookId
        AND n.deletedAt IS NULL
        AND n.folder IS NOT NULL
      """)
  List<Integer> findLiveNoteFolderIdsByNotebookId(@Param("notebookId") Integer notebookId);

  @Query(
      value =
          selectFromNote
              + " WHERE n.notebook.id = :notebookId AND n.deletedAt IS NULL"
              + " ORDER BY n.id ASC")
  List<Note> findLiveNotesByNotebookIdOrderByIdAsc(@Param("notebookId") Integer notebookId);

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
              + " WHERE n.notebook.ownership.user.id = :userId"
              + " AND n.deletedAt IS NULL"
              + " ORDER BY n.updatedAt DESC"
              + " LIMIT 20")
  List<Note> findRecentNotesByUser(@Param("userId") Integer userId);

  @Query(
      value =
          "SELECT DISTINCT n FROM Note n JOIN FETCH n.notebook LEFT JOIN FETCH n.folder "
              + "WHERE n.id IN :ids")
  List<Note> hydrateNonDeletedNotesWithNotebookAndFolderByIds(@Param("ids") List<Integer> ids);

  String recallWhereClause =
      " WHERE "
          + "   rp IS NULL "
          + "   AND COALESCE(n.recallSetting.skipMemoryTracking, FALSE) = FALSE "
          + "   AND n.deletedAt IS NULL ";

  String joinMemoryTracker =
      " LEFT JOIN n.memoryTrackers rp ON rp.user.id = :userId"
          + " AND rp.deletedAt IS NULL"
          + " AND "
          + MemoryTracker.JPA_WHERE_NOTE_LEVEL_TRACKER;

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

  @Query(
      value =
          "SELECT COUNT(nc) FROM NoteCreator nc WHERE nc.user.id = :userId AND nc.note.deletedAt"
              + " IS NULL")
  long countByCreator(@Param("userId") Integer userId);

  @Query(
      value =
          "SELECT MAX(nc.note.createdAt) FROM NoteCreator nc WHERE nc.user.id = :userId AND"
              + " nc.note.deletedAt IS NULL")
  java.sql.Timestamp findLastNoteTimeByCreator(@Param("userId") Integer userId);

  /**
   * Soft-deleted notes matching title in the same notebook + folder placement (null folder =
   * notebook root). Multiple rows are ordered by id ascending; callers take the first.
   */
  @Query(
      value =
          selectFromNote
              + " WHERE n.notebook.id = :notebookId AND n.deletedAt IS NOT NULL "
              + " AND LOWER(n.title) = LOWER(:title) "
              + " AND ((:folderId IS NULL AND n.folder IS NULL) "
              + " OR (:folderId IS NOT NULL AND n.folder IS NOT NULL AND n.folder.id = :folderId)) "
              + " ORDER BY n.id ASC")
  List<Note> findSoftDeletedByNotebookFolderAndTitleOrderByIdAsc(
      @Param("notebookId") Integer notebookId,
      @Param("folderId") Integer folderId,
      @Param("title") String title,
      Pageable pageable);

  @Query(
      value =
          "SELECT n.* FROM note n WHERE n.folder_id = :folderId AND n.deleted_at IS NULL "
              + "AND n.id NOT IN (:excludeIds) ORDER BY n.id ASC LIMIT :limit",
      nativeQuery = true)
  List<Note> findStructuralPeersInFolderOrderByIdAscLimited(
      @Param("folderId") Integer folderId,
      @Param("excludeIds") List<Integer> excludeIds,
      @Param("limit") int limit);

  @Query(
      value =
          "SELECT n.* FROM note n WHERE n.folder_id = :folderId AND n.deleted_at IS NULL "
              + "AND n.id NOT IN (:excludeIds) "
              + "ORDER BY CRC32(CONCAT(CAST(n.id AS CHAR), CAST(:seed AS CHAR))) ASC LIMIT :limit",
      nativeQuery = true)
  List<Note> findStructuralPeersInFolderOrderBySeedLimited(
      @Param("folderId") Integer folderId,
      @Param("excludeIds") List<Integer> excludeIds,
      @Param("seed") String seed,
      @Param("limit") int limit);

  @Query(
      value =
          "SELECT n.* FROM note n WHERE n.notebook_id = :notebookId AND n.folder_id IS NULL "
              + "AND n.deleted_at IS NULL AND n.id NOT IN (:excludeIds) ORDER BY n.id ASC LIMIT :limit",
      nativeQuery = true)
  List<Note> findStructuralPeersInNotebookRootOrderByIdAscLimited(
      @Param("notebookId") Integer notebookId,
      @Param("excludeIds") List<Integer> excludeIds,
      @Param("limit") int limit);

  @Query(
      value =
          "SELECT n.* FROM note n WHERE n.notebook_id = :notebookId AND n.folder_id IS NULL "
              + "AND n.deleted_at IS NULL AND n.id NOT IN (:excludeIds) "
              + "ORDER BY CRC32(CONCAT(CAST(n.id AS CHAR), CAST(:seed AS CHAR))) ASC LIMIT :limit",
      nativeQuery = true)
  List<Note> findStructuralPeersInNotebookRootOrderBySeedLimited(
      @Param("notebookId") Integer notebookId,
      @Param("excludeIds") List<Integer> excludeIds,
      @Param("seed") String seed,
      @Param("limit") int limit);
}
