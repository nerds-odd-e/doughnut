package com.odde.donut.services;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.Ownership;
import com.odde.donut.entities.User;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.notebookGit.NotebookGitCutoverService;
import java.sql.Timestamp;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotebookService {
  private final EntityPersister entityPersister;
  private final NotebookGitCutoverService notebookGitCutoverService;

  public NotebookService(
      EntityPersister entityPersister, NotebookGitCutoverService notebookGitCutoverService) {
    this.entityPersister = entityPersister;
    this.notebookGitCutoverService = notebookGitCutoverService;
  }

  @Transactional
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
    notebookGitCutoverService.createBindingForNotebook(notebook, currentUTCTimestamp.toInstant());
    return notebook;
  }
}
