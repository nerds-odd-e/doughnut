package com.odde.doughnut.services;

import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.factoryServices.EntityPersister;
import org.springframework.stereotype.Service;

@Service
public class NoteMotionService {
  private final EntityPersister entityPersister;

  public NoteMotionService(EntityPersister entityPersister) {
    this.entityPersister = entityPersister;
  }

  /** Places {@code source} in {@code targetFolder}. */
  public void executeMoveIntoFolder(Note source, Folder targetFolder) {
    Notebook targetNotebook = targetFolder.getNotebook();
    source.assignNotebook(targetNotebook);
    source.setFolder(targetFolder);
    entityPersister.flush();
    entityPersister.merge(source);
    entityPersister.flush();
  }

  /** Clears {@code subject}'s folder so it sits in notebook root. */
  public void executeMoveToNotebookRoot(Note subject) {
    subject.setFolder(null);
    entityPersister.flush();

    entityPersister.merge(subject);
    entityPersister.flush();
  }
}
