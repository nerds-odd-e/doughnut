package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.NotebookAiAssistant;
import com.odde.doughnut.entities.Ownership;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.entities.repositories.NotebookAiAssistantRepository;
import com.odde.doughnut.entities.repositories.NotebookRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class NotebookService {
  private final EntityPersister entityPersister;
  private final NotebookAiAssistantRepository notebookAiAssistantRepository;
  private final NoteRepository noteRepository;
  private final NotebookRepository notebookRepository;

  public NotebookService(
      EntityPersister entityPersister,
      NotebookAiAssistantRepository notebookAiAssistantRepository,
      NoteRepository noteRepository,
      NotebookRepository notebookRepository) {
    this.entityPersister = entityPersister;
    this.notebookAiAssistantRepository = notebookAiAssistantRepository;
    this.noteRepository = noteRepository;
    this.notebookRepository = notebookRepository;
  }

  public NotebookAiAssistant findByNotebookId(Integer notebookId) {
    return notebookAiAssistantRepository.findByNotebookId(notebookId);
  }

  public NotebookAiAssistant save(NotebookAiAssistant assistant) {
    return notebookAiAssistantRepository.save(assistant);
  }

  public Notebook createNotebookForOwnership(
      Ownership ownership,
      User user,
      Timestamp currentUTCTimestamp,
      String titleConstructor,
      String description) {
    Notebook notebook =
        ownership.prepareNotebookForNewNotebook(
            user, currentUTCTimestamp, titleConstructor, description);
    entityPersister.save(notebook);
    return notebook;
  }

  /**
   * Sets {@code notebook.index_note_id} from the sole root note titled {@code index}, or clears it
   * when none exists.
   */
  public void reconcileNotebookIndexNotePointer(Integer notebookId) {
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

  public Optional<Note> findOptionalIndexNote(Notebook notebook) {
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
      if (isValidNotebookIndexPointer(nb, cached)) {
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

  private static boolean isValidNotebookIndexPointer(Notebook notebook, Note note) {
    return note.getDeletedAt() == null
        && note.getFolder() == null
        && note.getNotebook() != null
        && Objects.equals(notebook.getId(), note.getNotebook().getId());
  }
}
