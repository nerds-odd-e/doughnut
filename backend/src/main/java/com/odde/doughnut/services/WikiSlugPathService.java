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

  static final String NOTE_SLUG_TMP_PREFIX = "nidtmp";

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
      note.setSlug(stableNoteSlug(note.getId()));
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
    Integer id = note.getId();
    if (id != null) {
      note.setSlug(stableNoteSlug(id));
      return;
    }
    Integer notebookId = note.getNotebook().getId();
    if (notebookId == null) {
      throw new IllegalStateException("notebook id required to assign note slug");
    }
    List<String> existing = findNoteSlugsInNotebookJdbc(notebookId, null);
    Set<String> taken = new HashSet<>(existing);
    note.setSlug(WikiSlugGeneration.uniqueSlugWithin(NOTE_SLUG_TMP_PREFIX, taken));
  }

  /**
   * After persist + flush so {@link Note#getId()} is assigned ({@code GenerationType.IDENTITY}).
   */
  public void finalizeNoteSlugAfterPersist(Note note) {
    String s = note.getSlug();
    if (s != null && s.startsWith(NOTE_SLUG_TMP_PREFIX)) {
      note.setSlug(stableNoteSlug(note.getId()));
    }
  }

  public static String stableNoteSlug(int noteId) {
    return "nid" + noteId;
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
}
