package com.odde.doughnut.services;

import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.factoryServices.EntityPersister;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class NoteMotionService {
  private final EntityPersister entityPersister;
  private final NotebookService notebookService;

  public NoteMotionService(EntityPersister entityPersister, NotebookService notebookService) {
    this.entityPersister = entityPersister;
    this.notebookService = notebookService;
  }

  /** Places {@code source} in {@code targetFolder}. */
  public void executeMoveIntoFolder(Note source, Folder targetFolder) {
    Integer notebookIdBefore = source.getNotebook().getId();
    Notebook targetNotebook = targetFolder.getNotebook();
    Integer notebookIdAfter = targetNotebook.getId();
    source.assignNotebook(targetNotebook);
    source.setFolder(targetFolder);
    entityPersister.flush();
    entityPersister.merge(source);
    entityPersister.flush();
    notebookService.reconcileNotebookIndexNotePointer(notebookIdBefore);
    if (!Objects.equals(notebookIdBefore, notebookIdAfter)) {
      notebookService.reconcileNotebookIndexNotePointer(notebookIdAfter);
    }
  }

  /** Clears {@code subject}'s folder so it sits in its current notebook's root. */
  public void executeMoveToNotebookRoot(Note subject) {
    executeMoveToNotebookRoot(subject, subject.getNotebook());
  }

  /** Assigns {@code source} to {@code targetNotebook} and clears folder (notebook root). */
  public void executeMoveToNotebookRoot(Note source, Notebook targetNotebook) {
    Integer notebookIdBefore = source.getNotebook().getId();
    Integer notebookIdAfter = targetNotebook.getId();
    source.assignNotebook(targetNotebook);
    source.setFolder(null);
    entityPersister.flush();
    entityPersister.merge(source);
    entityPersister.flush();
    notebookService.reconcileNotebookIndexNotePointer(notebookIdBefore);
    if (!Objects.equals(notebookIdBefore, notebookIdAfter)) {
      notebookService.reconcileNotebookIndexNotePointer(notebookIdAfter);
    }
  }
}
