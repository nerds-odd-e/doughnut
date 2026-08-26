package com.odde.donut.services;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.repositories.NotebookRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Profile("prod")
public class EmbeddingMaintenanceJob {
  private final NotebookRepository notebookRepository;
  private final NotebookIndexingService notebookIndexingService;

  public EmbeddingMaintenanceJob(
      NotebookRepository notebookRepository, NotebookIndexingService notebookIndexingService) {
    this.notebookRepository = notebookRepository;
    this.notebookIndexingService = notebookIndexingService;
  }

  @Scheduled(cron = "0 */5 * * * *")
  public void updateAllNotebooks() {
    for (Notebook nb : notebookRepository.findAll()) {
      notebookIndexingService.updateNotebookIndex(nb);
    }
  }
}
