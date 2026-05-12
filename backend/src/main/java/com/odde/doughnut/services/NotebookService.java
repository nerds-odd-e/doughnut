package com.odde.doughnut.services;

import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.NotebookAiAssistant;
import com.odde.doughnut.entities.Ownership;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NotebookAiAssistantRepository;
import com.odde.doughnut.entities.repositories.NotebookRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import java.sql.Timestamp;
import org.springframework.stereotype.Service;

@Service
public class NotebookService {
  private final EntityPersister entityPersister;
  private final NotebookAiAssistantRepository notebookAiAssistantRepository;
  private final NotebookRepository notebookRepository;

  public NotebookService(
      EntityPersister entityPersister,
      NotebookAiAssistantRepository notebookAiAssistantRepository,
      NotebookRepository notebookRepository) {
    this.entityPersister = entityPersister;
    this.notebookAiAssistantRepository = notebookAiAssistantRepository;
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
}
