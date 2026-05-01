package com.odde.doughnut.services;

import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.utils.WikiSlugGeneration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class WikiSlugPathService {

  private final JdbcTemplate jdbcTemplate;
  private final NoteRepository noteRepository;
  private final EntityPersister entityPersister;

  public WikiSlugPathService(
      JdbcTemplate jdbcTemplate, NoteRepository noteRepository, EntityPersister entityPersister) {
    this.jdbcTemplate = jdbcTemplate;
    this.noteRepository = noteRepository;
    this.entityPersister = entityPersister;
  }

  public void regenerateAllNoteSlugPaths() {
    for (Note note : noteRepository.findAllNonDeletedNotesOrderByNotebookFolderAndId()) {
      assignSlugForNewNote(note);
      entityPersister.merge(note);
      entityPersister.flush();
    }
  }

  public void assignSlugForNewFolder(Folder folder) {
    Integer notebookId = folder.getNotebook().getId();
    Integer parentFolderId =
        folder.getParentFolder() == null ? null : folder.getParentFolder().getId();
    Set<String> siblingBasenames =
        notebookId == null
            ? Set.of()
            : WikiSlugPathAssignment.basenamesFromSlugs(
                findFolderSlugsOfSiblingsJdbc(notebookId, parentFolderId));
    WikiSlugPathAssignment.setFolderSlug(folder, siblingBasenames);
  }

  public void assignSlugForNewFolderSkippingSiblingQuery(Folder folder) {
    WikiSlugPathAssignment.setFolderSlug(folder, Set.of());
  }

  public void assignSlugForNewNote(Note note) {
    Integer notebookId = note.getNotebook().getId();
    Folder folder = note.getFolder();
    Integer folderId = folder == null ? null : folder.getId();
    Integer excludeNoteId = note.getId();
    Set<String> siblingBasenames =
        notebookId == null
            ? Set.of()
            : WikiSlugPathAssignment.basenamesFromSlugs(
                findNoteSlugsInFolderScopeJdbc(notebookId, folderId, excludeNoteId));

    String folderSlug = folder == null ? null : folder.getSlug();
    if (folderSlug != null && folderSlug.length() >= Note.MAX_SLUG_LENGTH - 1) {
      assignNotebookUniqueFallbackSlug(note, notebookId, excludeNoteId);
      return;
    }

    WikiSlugPathAssignment.setNoteSlug(note, siblingBasenames);
    if (note.getSlug().length() > Note.MAX_SLUG_LENGTH) {
      assignNotebookUniqueFallbackSlug(note, notebookId, excludeNoteId);
    }
  }

  public void assignSlugForNewNoteSkippingSiblingQuery(Note note) {
    WikiSlugPathAssignment.setNoteSlug(note, Set.of());
  }

  private List<String> findFolderSlugsOfSiblingsJdbc(Integer notebookId, Integer parentFolderId) {
    if (parentFolderId == null) {
      return jdbcTemplate.queryForList(
          "SELECT slug FROM folder WHERE notebook_id = ? AND parent_folder_id IS NULL",
          String.class,
          notebookId);
    }
    return jdbcTemplate.queryForList(
        "SELECT slug FROM folder WHERE notebook_id = ? AND parent_folder_id = ?",
        String.class,
        notebookId,
        parentFolderId);
  }

  private List<String> findNoteSlugsInFolderScopeJdbc(
      Integer notebookId, Integer folderId, Integer excludeNoteId) {
    if (folderId == null) {
      if (excludeNoteId == null) {
        return jdbcTemplate.queryForList(
            "SELECT slug FROM note WHERE notebook_id = ? AND deleted_at IS NULL AND folder_id"
                + " IS NULL",
            String.class,
            notebookId);
      }
      return jdbcTemplate.queryForList(
          "SELECT slug FROM note WHERE notebook_id = ? AND deleted_at IS NULL AND folder_id IS"
              + " NULL AND id <> ?",
          String.class,
          notebookId,
          excludeNoteId);
    }
    if (excludeNoteId == null) {
      return jdbcTemplate.queryForList(
          "SELECT slug FROM note WHERE notebook_id = ? AND deleted_at IS NULL AND folder_id = ?",
          String.class,
          notebookId,
          folderId);
    }
    return jdbcTemplate.queryForList(
        "SELECT slug FROM note WHERE notebook_id = ? AND deleted_at IS NULL AND folder_id = ? AND"
            + " id <> ?",
        String.class,
        notebookId,
        folderId,
        excludeNoteId);
  }

  private List<String> findNoteSlugsInNotebookJdbc(Integer notebookId, Integer excludeNoteId) {
    if (excludeNoteId == null) {
      return jdbcTemplate.queryForList(
          "SELECT slug FROM note WHERE notebook_id = ? AND deleted_at IS NULL",
          String.class,
          notebookId);
    }
    return jdbcTemplate.queryForList(
        "SELECT slug FROM note WHERE notebook_id = ? AND deleted_at IS NULL AND id <> ?",
        String.class,
        notebookId,
        excludeNoteId);
  }

  private void assignNotebookUniqueFallbackSlug(
      Note note, Integer notebookId, Integer excludeNoteId) {
    List<String> existing = findNoteSlugsInNotebookJdbc(notebookId, excludeNoteId);
    Set<String> taken = new HashSet<>(existing);
    Integer id = note.getId();
    if (id != null) {
      note.setSlug(WikiSlugGeneration.uniqueSlugWithin("nid" + id, taken));
      return;
    }
    note.setSlug(WikiSlugGeneration.uniqueSlugWithin("nidtmp", taken));
  }
}
