package com.odde.doughnut.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.testability.MakeMe;
import java.util.List;
import java.util.Optional;
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
class NotebookReindexingServiceTests {

  @Mock EmbeddingService embeddingService;
  @Mock NoteEmbeddingService noteEmbeddingService;
  @Autowired MakeMe makeMe;

  NotebookReindexingService service;
  Notebook notebook;

  @BeforeEach
  void setup() {
    service = new NotebookReindexingService(embeddingService, noteEmbeddingService);
    notebook = makeMe.aNotebook().please();
    makeMe.aNote().under(notebook.getHeadNote()).please();
    makeMe.aNote().under(notebook.getHeadNote()).please();
    makeMe.refresh(notebook);
  }

  @Test
  void shouldDeleteOldEmbeddingsBeforeReindexing() {
    when(embeddingService.generateEmbedding(any(Note.class)))
        .thenReturn(Optional.of(List.of(1.0f, 2.0f, 3.0f)));

    service.reindexNotebook(notebook);

    verify(noteEmbeddingService).deleteNotebookEmbeddings(notebook.getId());
  }

  @Test
  void shouldGenerateEmbeddingsForAllNotesInNotebook() {
    when(embeddingService.generateEmbedding(any(Note.class)))
        .thenReturn(Optional.of(List.of(1.0f, 2.0f, 3.0f)));

    service.reindexNotebook(notebook);

    verify(embeddingService, times(3)).generateEmbedding(any(Note.class)); // head + 2 notes
    verify(noteEmbeddingService, times(3)).storeEmbedding(any(Note.class), any());
  }
}
