package com.odde.donut.entities.repositories;

import com.odde.donut.entities.DisplayName;
import com.odde.donut.entities.Folder;
import com.odde.donut.services.notebookTree.PortableTreeFolderRow;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface FolderRepository extends CrudRepository<Folder, Integer> {

  @Query(
      """
      SELECT NEW com.odde.donut.services.notebookTree.PortableTreeFolderRow(
          f.id, f.parentFolder.id, f.name, f.readmeContent)
      FROM Folder f WHERE f.notebook.id = :notebookId ORDER BY f.id ASC
      """)
  List<PortableTreeFolderRow> findPortableTreeRowsByNotebookId(
      @Param("notebookId") Integer notebookId);

  /** Folders directly holding a note or a file. */
  @Query(
      """
      SELECT f.id FROM Folder f
      WHERE f.notebook.id = :notebookId
        AND (EXISTS (SELECT 1 FROM Note n WHERE n.folder = f)
          OR EXISTS (SELECT 1 FROM NotebookAttachment a WHERE a.folder = f))
      """)
  Set<Integer> findOccupiedFolderIdsByNotebookId(@Param("notebookId") Integer notebookId);

  String FOLDER_SEARCHABLE = " AND f.notebook.deletedAt IS NULL AND f.trashedInDatabase = false ";
  String FOLDER_NAME_LIKE = " WHERE LOWER(f.name) LIKE LOWER(:pattern)" + FOLDER_SEARCHABLE;
  String FOLDER_NAME_EXACT = " WHERE LOWER(f.name) = LOWER(:key)" + FOLDER_SEARCHABLE;

  @Query("SELECT f FROM Folder f" + FOLDER_NAME_LIKE + " AND f.notebook.id = :notebookId")
  List<Folder> searchInNotebook(
      @Param("notebookId") Integer notebookId, @Param("pattern") String pattern, Pageable pageable);

  @Query("SELECT f FROM Folder f" + FOLDER_NAME_EXACT + " AND f.notebook.id = :notebookId")
  List<Folder> searchExactInNotebook(
      @Param("notebookId") Integer notebookId, @Param("key") String key);

  @Query(
      "SELECT f FROM Folder f" + FOLDER_NAME_LIKE + " AND f.notebook.ownership.user.id = :userId")
  List<Folder> searchForUserInAllMyNotebooks(
      @Param("userId") Integer userId, @Param("pattern") String pattern, Pageable pageable);

  @Query(
      "SELECT f FROM Folder f" + FOLDER_NAME_EXACT + " AND f.notebook.ownership.user.id = :userId")
  List<Folder> searchExactForUserInAllMyNotebooks(
      @Param("userId") Integer userId, @Param("key") String key);

  @Query(
      "SELECT f FROM Folder f JOIN f.notebook.subscriptions s ON s.user.id = :userId"
          + FOLDER_NAME_LIKE)
  List<Folder> searchForUserInAllMySubscriptions(
      @Param("userId") Integer userId, @Param("pattern") String pattern, Pageable pageable);

  @Query(
      "SELECT f FROM Folder f JOIN f.notebook.subscriptions s ON s.user.id = :userId"
          + FOLDER_NAME_EXACT)
  List<Folder> searchExactForUserInAllMySubscriptions(
      @Param("userId") Integer userId, @Param("key") String key);

  @Query(
      "SELECT f FROM Folder f JOIN f.notebook.ownership.circle.members m ON m.id = :userId"
          + FOLDER_NAME_LIKE)
  List<Folder> searchForUserInAllMyCircle(
      @Param("userId") Integer userId, @Param("pattern") String pattern, Pageable pageable);

  @Query(
      "SELECT f FROM Folder f JOIN f.notebook.ownership.circle.members m ON m.id = :userId"
          + FOLDER_NAME_EXACT)
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
      @Param("name") DisplayName name);

  @Query(
      """
      SELECT f FROM Folder f
      LEFT JOIN FETCH f.parentFolder
      WHERE f.notebook.id = :notebookId
      ORDER BY f.id ASC
      """)
  List<Folder> findByNotebookIdOrderByIdAsc(@Param("notebookId") Integer notebookId);
}
