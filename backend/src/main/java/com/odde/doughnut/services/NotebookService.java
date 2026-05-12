package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.NotebookAiAssistant;
import com.odde.doughnut.entities.Ownership;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NotebookAiAssistantRepository;
import com.odde.doughnut.entities.repositories.NotebookRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.index.IndexScope;
import com.odde.doughnut.services.index.ScopedIndexNoteService;
import java.sql.Timestamp;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class NotebookService {
  private final EntityPersister entityPersister;
  private final NotebookAiAssistantRepository notebookAiAssistantRepository;
  private final NotebookRepository notebookRepository;
  private final ScopedIndexNoteService scopedIndexNoteService;

  public NotebookService(
      EntityPersister entityPersister,
      NotebookAiAssistantRepository notebookAiAssistantRepository,
      NotebookRepository notebookRepository,
      ScopedIndexNoteService scopedIndexNoteService) {
    this.entityPersister = entityPersister;
    this.notebookAiAssistantRepository = notebookAiAssistantRepository;
    this.notebookRepository = notebookRepository;
    this.scopedIndexNoteService = scopedIndexNoteService;
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
   * Sets {@link Notebook#getIndexNote()} from the sole root note titled {@code index}, or clears it
   * when none exists.
   */
  public void reconcileNotebookIndexNotePointer(Integer notebookId) {
    if (notebookId == null) {
      return;
    }
    notebookRepository
        .findById(notebookId)
        .ifPresent(
            nb ->
                scopedIndexNoteService.reconcileDesignatedIndexPointer(
                    new IndexScope.NotebookRoot(nb)));
  }

  public Optional<Note> findOptionalIndexNote(Notebook notebook) {
    return scopedIndexNoteService.findDesignatedIndexNote(new IndexScope.NotebookRoot(notebook));
  }
}
