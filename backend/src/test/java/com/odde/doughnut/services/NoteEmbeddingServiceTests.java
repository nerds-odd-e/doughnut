package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.repositories.NoteEmbeddingRepository;
import com.odde.doughnut.testability.MakeMe;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NoteEmbeddingServiceTests {

  @Autowired NoteEmbeddingRepository noteEmbeddingRepository;
  // Use real DB via ModelFactoryService; no mocks here
  @Autowired MakeMe makeMe;

  NoteEmbeddingService service;
  Note note;
  Notebook notebook;

  @BeforeEach
  void setup() {
    service = new NoteEmbeddingService(makeMe.modelFactoryService);
    notebook = makeMe.aNotebook().please();
    note = makeMe.aNote().under(notebook.getHeadNote()).please();
  }

  @Test
  void shouldDeleteEmbeddingByNoteId() {
    makeMe.aNoteEmbedding(note).please();

    service.deleteEmbedding(note.getId());

    assertThat(noteEmbeddingRepository.existsByNoteId(note.getId()), is(false));
  }

  @Test
  void shouldDeleteNotebookEmbeddings() {
    makeMe.aNote().under(notebook.getHeadNote()).please();
    makeMe.refresh(notebook);
    notebook.getNotes().forEach(n -> makeMe.aNoteEmbedding(n).please());

    service.deleteNotebookEmbeddings(notebook.getId());

    notebook
        .getNotes()
        .forEach(n -> assertThat(noteEmbeddingRepository.existsByNoteId(n.getId()), is(false)));
  }

  @Test
  void shouldGetEmbeddingByNoteIdAndKind() {
    makeMe.aNoteEmbedding(note).embedding(List.of(1.0f, 2.0f, 3.0f)).please();

    Optional<List<Float>> result = service.getEmbedding(note.getId());

    assertThat(result.isPresent(), is(true));
    assertThat(result.get(), equalTo(List.of(1.0f, 2.0f, 3.0f)));
  }

  @Test
  void shouldReturnEmptyWhenEmbeddingNotFound() {
    Optional<List<Float>> result = service.getEmbedding(note.getId());

    assertThat(result.isPresent(), is(false));
  }
}
