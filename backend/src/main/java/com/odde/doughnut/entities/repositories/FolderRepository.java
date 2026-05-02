package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Folder;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface FolderRepository extends CrudRepository<Folder, Integer> {

  String folderNameLike =
      " WHERE LOWER(f.name) LIKE LOWER(:pattern) AND f.notebook.deletedAt IS NULL ";
  String folderNameExact = " WHERE LOWER(f.name) = LOWER(:key) AND f.notebook.deletedAt IS NULL ";

  @Query("SELECT f FROM Folder f" + folderNameLike + " AND f.notebook.id = :notebookId")
  List<Folder> searchInNotebook(
      @Param("notebookId") Integer notebookId, @Param("pattern") String pattern, Pageable pageable);

  @Query("SELECT f FROM Folder f" + folderNameExact + " AND f.notebook.id = :notebookId")
  List<Folder> searchExactInNotebook(
      @Param("notebookId") Integer notebookId, @Param("key") String key);

  @Query("SELECT f FROM Folder f" + folderNameLike + " AND f.notebook.ownership.user.id = :userId")
  List<Folder> searchForUserInAllMyNotebooks(
      @Param("userId") Integer userId, @Param("pattern") String pattern, Pageable pageable);

  @Query("SELECT f FROM Folder f" + folderNameExact + " AND f.notebook.ownership.user.id = :userId")
  List<Folder> searchExactForUserInAllMyNotebooks(
      @Param("userId") Integer userId, @Param("key") String key);

  @Query(
      "SELECT f FROM Folder f JOIN f.notebook.subscriptions s ON s.user.id = :userId"
          + folderNameLike)
  List<Folder> searchForUserInAllMySubscriptions(
      @Param("userId") Integer userId, @Param("pattern") String pattern, Pageable pageable);

  @Query(
      "SELECT f FROM Folder f JOIN f.notebook.subscriptions s ON s.user.id = :userId"
          + folderNameExact)
  List<Folder> searchExactForUserInAllMySubscriptions(
      @Param("userId") Integer userId, @Param("key") String key);

  @Query(
      "SELECT f FROM Folder f JOIN f.notebook.ownership.circle.members m ON m.id = :userId"
          + folderNameLike)
  List<Folder> searchForUserInAllMyCircle(
      @Param("userId") Integer userId, @Param("pattern") String pattern, Pageable pageable);

  @Query(
      "SELECT f FROM Folder f JOIN f.notebook.ownership.circle.members m ON m.id = :userId"
          + folderNameExact)
  List<Folder> searchExactForUserInAllMyCircle(
      @Param("userId") Integer userId, @Param("key") String key);

  @Query(
      """
      SELECT f FROM Folder f
      WHERE f.notebook.id = :notebookId AND f.parentFolder IS NULL
      ORDER BY f.id ASC
      """)
  List<Folder> findRootFoldersByNotebookIdOrderByIdAsc(@Param("notebookId") Integer notebookId);

  @Query(
      """
      SELECT f FROM Folder f
      WHERE f.parentFolder.id = :parentFolderId
      ORDER BY f.id ASC
      """)
  List<Folder> findChildFoldersByParentFolderIdOrderByIdAsc(
      @Param("parentFolderId") Integer parentFolderId);

  @Query(
      """
      SELECT f FROM Folder f WHERE f.notebook.id = :notebookId AND f.name = :name
      AND ((:parentFolderId IS NULL AND f.parentFolder IS NULL)
           OR (f.parentFolder IS NOT NULL AND f.parentFolder.id = :parentFolderId))
      ORDER BY f.id ASC
      """)
  List<Folder> findCandidateChildContainers(
      @Param("notebookId") Integer notebookId,
      @Param("parentFolderId") Integer parentFolderId,
      @Param("name") String name);
}
