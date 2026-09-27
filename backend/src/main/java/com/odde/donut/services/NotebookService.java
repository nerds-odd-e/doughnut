package com.odde.donut.services;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.Ownership;
import com.odde.donut.entities.User;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.notebookGit.NotebookGitHistoryService;
import java.sql.Timestamp;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotebookService {
  private final EntityPersister entityPersister;
  private final NotebookGitHistoryService notebookGitHistoryService;

  public NotebookService(
      EntityPersister entityPersister, NotebookGitHistoryService notebookGitHistoryService) {
    this.entityPersister = entityPersister;
    this.notebookGitHistoryService = notebookGitHistoryService;
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
    notebookGitHistoryService.startHistory(notebook, currentUTCTimestamp.toInstant());
    return notebook;
  }
}
