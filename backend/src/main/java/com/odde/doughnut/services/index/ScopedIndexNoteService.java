package com.odde.doughnut.services.index;

import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.entities.repositories.NotebookRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScopedIndexNoteService {

  private final EntityPersister entityPersister;
  private final NoteRepository noteRepository;
  private final NotebookRepository notebookRepository;

  public ScopedIndexNoteService(
      EntityPersister entityPersister,
      NoteRepository noteRepository,
      NotebookRepository notebookRepository) {
    this.entityPersister = entityPersister;
    this.noteRepository = noteRepository;
    this.notebookRepository = notebookRepository;
  }

  /**
   * Resolves the designated index note for {@code scope}. Folder scope returns empty until folder
   * index persistence exists (Phase 10.5).
   */
  @Transactional
  public Optional<Note> findDesignatedIndexNote(IndexScope scope) {
    return switch (scope) {
      case IndexScope.NotebookRoot nr -> findNotebookRootIndexNote(nr.notebook());
      case IndexScope.FolderIndex _ -> Optional.empty();
    };
  }

  /**
   * Sets {@link Notebook#getIndexNote()} from the sole root note titled {@code index}, or clears it
   * when none exists. Folder scope is a no-op until Phase 10.5.
   */
  @Transactional
  public void reconcileDesignatedIndexPointer(IndexScope scope) {
    switch (scope) {
      case IndexScope.NotebookRoot nr -> reconcileNotebookRootPointer(nr.notebook().getId());
      case IndexScope.FolderIndex _ -> {}
    }
  }

  /**
   * Whether {@code note} is structurally valid as the designated index for {@code scope}. Used for
   * validation and future pointer checks; folder scope does not require a cached FK on {@link
   * Folder} yet.
   */
  public boolean isDesignatedIndexNote(IndexScope scope, Note note) {
    if (note == null || note.getDeletedAt() != null) {
      return false;
    }
    return switch (scope) {
      case IndexScope.NotebookRoot nr -> isValidNotebookRootIndexNote(nr.notebook(), note);
      case IndexScope.FolderIndex fi -> isValidFolderIndexNote(fi.folder(), note);
    };
  }

  private Optional<Note> findNotebookRootIndexNote(Notebook notebook) {
    if (notebook == null || notebook.getId() == null) {
      return Optional.empty();
    }
    Optional<Notebook> reloaded = notebookRepository.findById(notebook.getId());
    if (reloaded.isEmpty()) {
      return Optional.empty();
    }
    Notebook nb = reloaded.get();
    entityPersister.refresh(nb);
    Note cached = nb.getIndexNote();
    if (cached != null) {
      if (isValidNotebookRootIndexNote(nb, cached)) {
        return Optional.of(cached);
      }
      nb.setIndexNote(null);
      entityPersister.merge(nb);
      entityPersister.flush();
    }
    List<Note> found =
        noteRepository.findRootIndexNoteCandidatesForNotebook(nb.getId(), PageRequest.of(0, 2));
    if (found.isEmpty()) {
      return Optional.empty();
    }
    Note candidate = found.getFirst();
    nb.setIndexNote(candidate);
    entityPersister.merge(nb);
    entityPersister.flush();
    return Optional.of(candidate);
  }

  private void reconcileNotebookRootPointer(Integer notebookId) {
    if (notebookId == null) {
      return;
    }
    notebookRepository
        .findById(notebookId)
        .ifPresent(
            nb -> {
              List<Note> candidates =
                  noteRepository.findRootIndexNoteCandidatesForNotebook(
                      nb.getId(), PageRequest.of(0, 2));
              nb.setIndexNote(candidates.isEmpty() ? null : candidates.getFirst());
              entityPersister.merge(nb);
            });
  }

  private static boolean isValidNotebookRootIndexNote(Notebook notebook, Note note) {
    return note.getDeletedAt() == null
        && note.getFolder() == null
        && note.getNotebook() != null
        && Objects.equals(notebook.getId(), note.getNotebook().getId());
  }

  private static boolean isValidFolderIndexNote(Folder folder, Note note) {
    if (folder == null
        || folder.getId() == null
        || note.getNotebook() == null
        || note.getFolder() == null) {
      return false;
    }
    return Objects.equals(folder.getNotebook().getId(), note.getNotebook().getId())
        && Objects.equals(folder.getId(), note.getFolder().getId());
  }
}
