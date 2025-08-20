package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteEmbedding;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.repositories.NoteEmbeddingRepository;
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
  @Autowired NoteEmbeddingService noteEmbeddingService;
  @Autowired NoteEmbeddingRepository noteEmbeddingRepository;
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
    // Arrange: seed some embeddings to be deleted and mock generation
    when(embeddingService.generateEmbedding(any(Note.class)))
        .thenReturn(Optional.of(List.of(1.0f, 2.0f, 3.0f)));
    // seed previous embeddings
    notebook.getNotes().forEach(n -> makeMe.aNoteEmbedding(n).please());

    service.reindexNotebook(notebook);

    // Assert: all embeddings were cleared and regenerated once per note
    notebook
        .getNotes()
        .forEach(
            n ->
                assertThat(
                    noteEmbeddingRepository
                        .findByNoteIdAndKind(n.getId(), NoteEmbedding.EmbeddingKind.TITLE)
                        .isPresent(),
                    is(true)));
  }

  @Test
  void shouldGenerateEmbeddingsForAllNotesInNotebook() {
    when(embeddingService.generateEmbedding(any(Note.class)))
        .thenReturn(Optional.of(List.of(1.0f, 2.0f, 3.0f)));

    service.reindexNotebook(notebook);

    int numNotes = notebook.getNotes().size();
    long regeneratedCount =
        notebook.getNotes().stream()
            .filter(
                n ->
                    noteEmbeddingRepository
                        .findByNoteIdAndKind(n.getId(), NoteEmbedding.EmbeddingKind.TITLE)
                        .isPresent())
            .count();
    assertThat((int) regeneratedCount, equalTo(numNotes));
  }

  @Test
  void shouldStoreDetailsEmbeddingForNotesWithDetailsOnReindex() {
    // One note with details (default), one more with details explicitly
    when(embeddingService.generateEmbedding(any(Note.class)))
        .thenReturn(Optional.of(List.of(1.0f, 2.0f, 3.0f)));

    // Ensure at least one child has non-empty details
    makeMe.theNote(notebook.getHeadNote()).details("Has details").please();
    makeMe.refresh(notebook);

    service.reindexNotebook(notebook);

    notebook
        .getNotes()
        .forEach(
            n ->
                assertThat(
                    noteEmbeddingRepository
                        .findByNoteIdAndKind(n.getId(), NoteEmbedding.EmbeddingKind.DETAILS)
                        .isPresent(),
                    is(true)));
  }

  @Test
  void shouldNotStoreDetailsEmbeddingForNotesWithoutDetailsOnReindex() {
    when(embeddingService.generateEmbedding(any(Note.class)))
        .thenReturn(Optional.of(List.of(1.0f, 2.0f, 3.0f)));

    // Create a fresh notebook with notes lacking details
    Notebook nb = makeMe.aNotebook().please();
    // Ensure head note also has empty details
    makeMe.theNote(nb.getHeadNote()).withNoDescription().please();
    makeMe.aNote().under(nb.getHeadNote()).withNoDescription().please();
    makeMe.aNote().under(nb.getHeadNote()).withNoDescription().please();
    makeMe.refresh(nb);

    service.reindexNotebook(nb);

    nb.getNotes()
        .forEach(
            n ->
                assertThat(
                    noteEmbeddingRepository
                        .findByNoteIdAndKind(n.getId(), NoteEmbedding.EmbeddingKind.DETAILS)
                        .isPresent(),
                    is(false)));
  }
}
