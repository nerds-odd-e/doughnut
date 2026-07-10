package com.odde.doughnut.services;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.repositories.NotebookRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmbeddingMaintenanceJobTests {

  NotebookRepository notebookRepository;
  NotebookIndexingService notebookIndexingService;
  EmbeddingMaintenanceJob job;

  @BeforeEach
  void setup() {
    notebookRepository = mock(NotebookRepository.class);
    notebookIndexingService = mock(NotebookIndexingService.class);
    job = new EmbeddingMaintenanceJob(notebookRepository, notebookIndexingService);
  }

  @Test
  void shouldInvokeUpdateForEveryNotebook() {
    Notebook nb1 = mock(Notebook.class);
    Notebook nb2 = mock(Notebook.class);
    when(notebookRepository.findAll()).thenReturn(List.of(nb1, nb2));

    job.updateAllNotebooks();

    verify(notebookIndexingService).updateNotebookIndex(nb1);
    verify(notebookIndexingService).updateNotebookIndex(nb2);
  }
}
