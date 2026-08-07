package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Note;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Native queries that sample structural peer notes for accidental-match / health flows. */
public interface NoteStructuralPeerQueries {

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
