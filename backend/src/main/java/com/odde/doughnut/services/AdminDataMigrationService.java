package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.AdminDataMigrationStatusDTO;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.factoryServices.EntityPersister;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDataMigrationService {

  private final JdbcTemplate jdbcTemplate;
  private final WikiSlugPathService wikiSlugPathService;
  private final EntityPersister entityPersister;

  private AdminDataMigrationStatusDTO lastSuccessfulStatus =
      idleStatus(new AdminDataMigrationStatusDTO());

  public AdminDataMigrationService(
      JdbcTemplate jdbcTemplate,
      WikiSlugPathService wikiSlugPathService,
      EntityPersister entityPersister) {
    this.jdbcTemplate = jdbcTemplate;
    this.wikiSlugPathService = wikiSlugPathService;
    this.entityPersister = entityPersister;
  }

  public synchronized AdminDataMigrationStatusDTO getStatus() {
    return copy(lastSuccessfulStatus);
  }

  @Transactional(timeout = 600)
  public synchronized AdminDataMigrationStatusDTO run() {
    AdminDataMigrationStatusDTO dto = idleStatus(new AdminDataMigrationStatusDTO());

    List<int[]> notebookIndexPairs =
        jdbcTemplate.query(
            """
            SELECT nh.notebook_id, nh.head_note_id
            FROM notebook_head_note nh
            INNER JOIN notebook n ON n.id = nh.notebook_id
            """,
            (rs, rowNum) -> new int[] {rs.getInt("notebook_id"), rs.getInt("head_note_id")});

    int detachedChildFolders = 0;
    int normalNotes = 0;
    int relationNotes = 0;
    int deletedRoots = 0;

    for (int[] pair : notebookIndexPairs) {
      int notebookId = pair[0];
      int indexNoteId = pair[1];

      List<Integer> indexFolderIds =
          jdbcTemplate.queryForList(
              """
              SELECT DISTINCT n.folder_id FROM note n
               WHERE n.notebook_id = ?
                 AND n.deleted_at IS NULL
                 AND n.parent_id = ?
                 AND n.folder_id IS NOT NULL
              ORDER BY folder_id ASC
              """,
              Integer.class,
              notebookId,
              indexNoteId);
      indexFolderIds = indexFolderIds.stream().filter(Objects::nonNull).distinct().toList();

      if (!indexFolderIds.isEmpty()) {
        String placeholders =
            indexFolderIds.stream().map(id -> "?").collect(Collectors.joining(","));
        List<Object> detachArgs = new ArrayList<>();
        detachArgs.add(notebookId);
        detachArgs.addAll(indexFolderIds);
        detachedChildFolders +=
            jdbcTemplate.update(
                """
                UPDATE folder f
                   SET parent_folder_id = NULL
                 WHERE f.notebook_id = ?
                   AND f.parent_folder_id IN ("""
                    + placeholders
                    + ")",
                detachArgs.toArray());

        placeholders = indexFolderIds.stream().map(id -> "?").collect(Collectors.joining(","));
        List<Object> normalArgs = new ArrayList<>();
        normalArgs.add(notebookId);
        normalArgs.add(indexNoteId);
        normalArgs.addAll(indexFolderIds);
        normalNotes +=
            jdbcTemplate.update(
                """
                UPDATE note n
                   SET n.parent_id = NULL, n.folder_id = NULL
                 WHERE n.notebook_id = ?
                   AND n.parent_id = ?
                   AND n.target_note_id IS NULL
                   AND n.folder_id IN ("""
                    + placeholders
                    + ")",
                normalArgs.toArray());

        placeholders = indexFolderIds.stream().map(id -> "?").collect(Collectors.joining(","));
        List<Object> relArgs = new ArrayList<>();
        relArgs.add(notebookId);
        relArgs.add(indexNoteId);
        relArgs.addAll(indexFolderIds);
        relationNotes +=
            jdbcTemplate.update(
                """
                UPDATE note n SET n.folder_id = NULL
                 WHERE n.notebook_id = ?
                   AND n.parent_id = ?
                   AND n.target_note_id IS NOT NULL
                   AND n.folder_id IN ("""
                    + placeholders
                    + ")",
                relArgs.toArray());
      }

      List<Integer> obsoleteRootFolderIds =
          jdbcTemplate.queryForList(
              """
              SELECT f.id FROM folder f
               INNER JOIN notebook n ON n.id = f.notebook_id
               WHERE n.id = ?
                 AND f.parent_folder_id IS NULL
                 AND BINARY TRIM(COALESCE(f.name, '')) = BINARY TRIM(COALESCE(n.name, ''))
                 AND LENGTH(TRIM(f.name)) > 0
                 AND NOT EXISTS (SELECT 1 FROM note pn WHERE pn.folder_id = f.id)
                 AND NOT EXISTS (SELECT 1 FROM folder pf WHERE pf.parent_folder_id = f.id)
              """,
              Integer.class,
              notebookId);
      if (!obsoleteRootFolderIds.isEmpty()) {
        String delPlaceholders =
            obsoleteRootFolderIds.stream().map(id -> "?").collect(Collectors.joining(","));
        deletedRoots +=
            jdbcTemplate.update(
                "DELETE FROM folder WHERE id IN (" + delPlaceholders + ")",
                obsoleteRootFolderIds.toArray());
      }
    }

    entityPersister.flush();
    regenerateAllSlugPaths();

    dto.setDetachedChildFoldersFromIndexFolder(detachedChildFolders);
    dto.setUpdatedNormalNotesDetachedFromIndex(normalNotes);
    dto.setUpdatedRelationNotesClearedFolder(relationNotes);
    dto.setDeletedObsoleteNotebookNameRootFolders(deletedRoots);
    dto.setNotebookCountSlugScan(
        jdbcTemplate.queryForObject("SELECT COUNT(DISTINCT id) FROM notebook", Long.class));

    dto.setCompletedOnce(true);
    dto.setLastCompletedAt(Instant.now());
    dto.setMessage(summaryMessage(dto));
    lastSuccessfulStatus = copy(dto);
    return copy(dto);
  }

  private void regenerateAllSlugPaths() {
    jdbcTemplate.update("UPDATE folder SET slug = CONCAT('z-mig-f-', id)");
    jdbcTemplate.update("UPDATE note SET slug = CONCAT('z-mig-n-', id)");
    entityPersister.flushAndClear();

    List<Integer> ordered =
        jdbcTemplate.queryForList("SELECT id FROM notebook ORDER BY id ASC", Integer.class);

    for (Integer notebookId : ordered) {
      regenerateFolderTreeSlugs(notebookId);
      entityPersister.flush();
      regenerateNotebookNoteSlugs(notebookId);
      entityPersister.flush();
      entityPersister.flushAndClear();
    }
  }

  private void regenerateFolderTreeSlugs(Integer notebookId) {
    List<Integer> rootIds =
        jdbcTemplate.queryForList(
            "SELECT id FROM folder WHERE notebook_id = ? AND parent_folder_id IS NULL ORDER BY id",
            Integer.class,
            notebookId);

    ArrayDeque<Integer> queue = new ArrayDeque<>(rootIds);
    Set<Integer> scheduled = new HashSet<>(queue);

    while (!queue.isEmpty()) {
      Integer folderId = queue.poll();
      Folder folder = entityPersister.find(Folder.class, folderId);
      wikiSlugPathService.assignSlugForNewFolder(folder);
      entityPersister.merge(folder);

      List<Integer> childIds =
          jdbcTemplate.queryForList(
              "SELECT id FROM folder WHERE parent_folder_id = ? ORDER BY id ASC",
              Integer.class,
              folderId);

      for (Integer childId : childIds) {
        if (scheduled.add(childId)) {
          queue.addLast(childId);
        }
      }
    }
  }

  private void regenerateNotebookNoteSlugs(Integer notebookId) {
    List<Integer> noteIds =
        jdbcTemplate.queryForList(
            "SELECT id FROM note WHERE notebook_id = ? ORDER BY id ASC", Integer.class, notebookId);

    for (Integer noteId : noteIds) {
      Note note = entityPersister.find(Note.class, noteId);
      wikiSlugPathService.assignSlugDuringDataMigration(note);
      entityPersister.merge(note);
    }
  }

  private static AdminDataMigrationStatusDTO idleStatus(AdminDataMigrationStatusDTO dto) {
    dto.setCompletedOnce(false);
    dto.setLastCompletedAt(null);
    dto.setMessage("No migration has completed in this server process yet.");
    dto.setDetachedChildFoldersFromIndexFolder(0);
    dto.setUpdatedNormalNotesDetachedFromIndex(0);
    dto.setUpdatedRelationNotesClearedFolder(0);
    dto.setDeletedObsoleteNotebookNameRootFolders(0);
    dto.setNotebookCountSlugScan(0);
    return dto;
  }

  private static String summaryMessage(AdminDataMigrationStatusDTO dto) {
    return "Detached child folders "
        + dto.getDetachedChildFoldersFromIndexFolder()
        + "; moved normal notes "
        + dto.getUpdatedNormalNotesDetachedFromIndex()
        + "; cleared relation note folders "
        + dto.getUpdatedRelationNotesClearedFolder()
        + "; deleted obsolete notebook-name root folders "
        + dto.getDeletedObsoleteNotebookNameRootFolders()
        + "; slug regen sc "
        + dto.getNotebookCountSlugScan()
        + " notebooks.";
  }

  private static AdminDataMigrationStatusDTO copy(AdminDataMigrationStatusDTO source) {
    AdminDataMigrationStatusDTO d = new AdminDataMigrationStatusDTO();
    d.setCompletedOnce(source.isCompletedOnce());
    d.setLastCompletedAt(source.getLastCompletedAt());
    d.setMessage(source.getMessage());
    d.setDetachedChildFoldersFromIndexFolder(source.getDetachedChildFoldersFromIndexFolder());
    d.setUpdatedNormalNotesDetachedFromIndex(source.getUpdatedNormalNotesDetachedFromIndex());
    d.setUpdatedRelationNotesClearedFolder(source.getUpdatedRelationNotesClearedFolder());
    d.setDeletedObsoleteNotebookNameRootFolders(source.getDeletedObsoleteNotebookNameRootFolders());
    d.setNotebookCountSlugScan(source.getNotebookCountSlugScan());
    return d;
  }
}
