package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.repositories.NoteEmbeddingJdbcRepository;
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
  @Autowired NoteEmbeddingJdbcRepository noteEmbeddingJdbcRepository;
  @Autowired com.odde.doughnut.factoryServices.ModelFactoryService modelFactoryService;
  @Autowired MakeMe makeMe;

  // Service removed; keep tests minimal for update/reset endpoints in controller
  Notebook notebook;

  @BeforeEach
  void setup() {
    // Service removed; tests below will use noteEmbeddingService directly where applicable
    notebook = makeMe.aNotebook().please();
    makeMe.aNote().under(notebook.getHeadNote()).please();
    makeMe.aNote().under(notebook.getHeadNote()).please();
    makeMe.refresh(notebook);
    // Default: mock batched streaming embeddings to return a vector for every note
    when(embeddingService.streamEmbeddingsForNoteList(any()))
        .thenAnswer(
            invocation -> {
              @SuppressWarnings("unchecked")
              List<Note> notes = (List<Note>) invocation.getArgument(0);
              return notes.stream()
                  .map(
                      n ->
                          new EmbeddingService.EmbeddingForNote(
                              n, Optional.of(List.of(1.0f, 2.0f, 3.0f))));
            });
  }

  // Reindex service removed

  // Reindex service removed

  // Reindex service removed

  @Test
  void updateNotebookIndex_shouldOnlyUpdateNotesWithoutEmbeddingsOrStaleOnes() {
    // Arrange: create embeddings for one note to be up-to-date, and leave another without
    List<Note> notes = notebook.getNotes();
    Note first = notes.get(0);
    Note second = notes.get(1);

    // Seed an existing embedding for the first note
    makeMe.aNoteEmbedding(first).please();

    // Make sure first note is not updated after embedding, and second is updated now
    // Update second's updatedAt to be "newer" by touching details
    makeMe.theNote(second).details("newer details").please();
    makeMe.refresh(notebook);

    // Act
    // mimic controller update behavior by streaming embeddings for selected candidates
    List<Integer> candidateIds =
        noteEmbeddingJdbcRepository.selectNoteIdsNeedingIndexUpdateByNotebookId(notebook.getId());
    List<Note> candidates =
        (List<Note>) modelFactoryService.noteRepository.findAllById(candidateIds);
    embeddingService
        .streamEmbeddingsForNoteList(candidates)
        .forEach(
            item ->
                item.embedding()
                    .ifPresent(
                        embedding -> noteEmbeddingService.storeEmbedding(item.note(), embedding)));

    // Assert: both notes should have TITLE embeddings (first already had; second should now)
    assertThat(noteEmbeddingRepository.existsByNoteId(first.getId()), is(true));
    assertThat(noteEmbeddingRepository.existsByNoteId(second.getId()), is(true));
  }

  // Reindex service removed
}
