package com.odde.donut.services;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.Ownership;
import com.odde.donut.entities.User;
import com.odde.donut.factoryServices.EntityPersister;
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
