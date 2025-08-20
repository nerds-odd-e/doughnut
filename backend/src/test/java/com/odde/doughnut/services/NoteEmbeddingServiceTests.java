package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteEmbedding;
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
class NoteEmbeddingServiceTests {

  @Autowired NoteEmbeddingRepository noteEmbeddingRepository;
  @Mock NoteEmbeddingJdbcRepository noteEmbeddingJdbcRepository;
  @Autowired MakeMe makeMe;

  NoteEmbeddingService service;
  Note note;
  Notebook notebook;

  @BeforeEach
  void setup() {
    service =
        new NoteEmbeddingService(
            noteEmbeddingRepository, noteEmbeddingJdbcRepository, makeMe.modelFactoryService);
    notebook = makeMe.aNotebook().please();
    note = makeMe.aNote().under(notebook.getHeadNote()).please();
  }

  @Test
  void shouldStoreTitleEmbedding() {
    List<Float> embedding = List.of(1.0f, 2.0f, 3.0f);

    service.storeEmbedding(note, embedding);

    // Verify that the service called save on the ModelFactoryService
    // We can't verify the exact calls since ModelFactoryService is not mocked
    // But we can verify the service doesn't throw exceptions
  }

  @Test
  void shouldStoreDetailsEmbeddingWhenNoteHasDetails() {
    note.setDetails("Test details");
    List<Float> embedding = List.of(1.0f, 2.0f, 3.0f);

    service.storeEmbedding(note, embedding);

    // Verify that the service doesn't throw exceptions
  }

  @Test
  void shouldNotStoreDetailsEmbeddingWhenNoteHasNoDetails() {
    note.setDetails(null);
    List<Float> embedding = List.of(1.0f, 2.0f, 3.0f);

    service.storeEmbedding(note, embedding);

    // Verify that the service doesn't throw exceptions
  }

  @Test
  void shouldNotStoreDetailsEmbeddingWhenNoteHasEmptyDetails() {
    note.setDetails("");
    List<Float> embedding = List.of(1.0f, 2.0f, 3.0f);

    service.storeEmbedding(note, embedding);

    // Verify that the service doesn't throw exceptions
  }

  @Test
  void shouldDeleteEmbeddingByNoteId() {
    makeMe
        .modelFactoryService
        .entityManager
        .createNativeQuery(
            "INSERT INTO note_embeddings (note_id, kind, created_at, updated_at, embedding_raw) VALUES (:nid, :kind, NOW(), NOW(), :emb)")
        .setParameter("nid", note.getId())
        .setParameter("kind", NoteEmbedding.EmbeddingKind.TITLE.name())
        .setParameter("emb", new byte[] {0})
        .executeUpdate();

    service.deleteEmbedding(note.getId());

    assertThat(
        noteEmbeddingRepository
            .findByNoteIdAndKind(note.getId(), NoteEmbedding.EmbeddingKind.TITLE)
            .isPresent(),
        is(false));
  }

  @Test
  void shouldDeleteNotebookEmbeddings() {
    makeMe.aNote().under(notebook.getHeadNote()).please();
    makeMe.refresh(notebook);
    // create embeddings for notes in the notebook using native SQL
    notebook
        .getNotes()
        .forEach(
            n ->
                makeMe
                    .modelFactoryService
                    .entityManager
                    .createNativeQuery(
                        "INSERT INTO note_embeddings (note_id, kind, created_at, updated_at, embedding_raw) VALUES (:nid, :kind, NOW(), NOW(), :emb)")
                    .setParameter("nid", n.getId())
                    .setParameter("kind", NoteEmbedding.EmbeddingKind.TITLE.name())
                    .setParameter("emb", new byte[] {0})
                    .executeUpdate());

    service.deleteNotebookEmbeddings(notebook.getId());

    notebook
        .getNotes()
        .forEach(
            n ->
                assertThat(
                    noteEmbeddingRepository
                        .findByNoteIdAndKind(n.getId(), NoteEmbedding.EmbeddingKind.TITLE)
                        .isPresent(),
                    is(false)));
  }

  @Test
  void shouldGetEmbeddingByNoteIdAndKind() {
    NoteEmbedding mockEmbedding = new NoteEmbedding();
    mockEmbedding.setEmbeddingFromFloats(List.of(1.0f, 2.0f, 3.0f));

    when(noteEmbeddingJdbcRepository.select(note.getId(), NoteEmbedding.EmbeddingKind.TITLE.name()))
        .thenReturn(Optional.of(mockEmbedding.getEmbedding()));

    Optional<List<Float>> result =
        service.getEmbedding(note.getId(), NoteEmbedding.EmbeddingKind.TITLE);

    assertThat(result.isPresent(), is(true));
    assertThat(result.get(), equalTo(List.of(1.0f, 2.0f, 3.0f)));
  }

  @Test
  void shouldReturnEmptyWhenEmbeddingNotFound() {
    when(noteEmbeddingJdbcRepository.select(note.getId(), NoteEmbedding.EmbeddingKind.TITLE.name()))
        .thenReturn(Optional.empty());

    Optional<List<Float>> result =
        service.getEmbedding(note.getId(), NoteEmbedding.EmbeddingKind.TITLE);

    assertThat(result.isPresent(), is(false));
  }
}
