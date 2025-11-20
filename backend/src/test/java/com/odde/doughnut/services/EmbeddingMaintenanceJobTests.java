package com.odde.doughnut.services;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.repositories.NotebookRepository;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EmbeddingMaintenanceJobTests {

  @Autowired MakeMe makeMe;
  @Autowired NotebookRepository notebookRepository;

  @Mock NotebookIndexingService notebookIndexingService;

  EmbeddingMaintenanceJob job;

  Notebook nb1;
  Notebook nb2;

  @BeforeEach
  void setup() {
    nb1 = makeMe.aNotebook().please();
    nb2 = makeMe.aNotebook().please();
    // Add a couple of notes to each to simulate content
    makeMe.aNote().under(nb1.getHeadNote()).please();
    makeMe.aNote().under(nb2.getHeadNote()).please();

    job = new EmbeddingMaintenanceJob(notebookRepository, notebookIndexingService);
  }

  @Test
  void shouldInvokeUpdateForEveryNotebook() {
    job.updateAllNotebooks();

    verify(notebookIndexingService, times(1)).updateNotebookIndex(nb1);
    verify(notebookIndexingService, times(1)).updateNotebookIndex(nb2);
  }
}
