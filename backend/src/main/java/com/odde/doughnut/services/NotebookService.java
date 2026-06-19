package com.odde.doughnut.services;

import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.Ownership;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.EntityPersister;
import java.sql.Timestamp;
import org.springframework.stereotype.Service;

@Service
public class NotebookService {
  private final EntityPersister entityPersister;

  public NotebookService(EntityPersister entityPersister) {
    this.entityPersister = entityPersister;
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
}
