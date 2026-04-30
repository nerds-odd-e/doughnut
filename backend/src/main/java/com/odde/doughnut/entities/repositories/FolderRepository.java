package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Folder;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface FolderRepository extends CrudRepository<Folder, Integer> {

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
