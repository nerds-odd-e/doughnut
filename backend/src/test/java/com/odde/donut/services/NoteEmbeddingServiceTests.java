package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.repositories.NoteEmbeddingRepository;
import com.odde.donut.testability.SpringTestBase;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NoteEmbeddingServiceTests extends SpringTestBase {
  @Autowired NoteEmbeddingRepository noteEmbeddingRepository;
  @Autowired NoteEmbeddingService service;

  Note note;

  @BeforeEach
  void setup() {
    note = makeMe.aNote().please();
  }

  @Test
  void shouldDeleteEmbeddingByNoteId() {
    makeMe.aNoteEmbedding(note).please();

    service.deleteEmbedding(note.getId());

    assertThat(noteEmbeddingRepository.existsByNoteId(note.getId()), is(false));
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
    assertThat(service.getEmbedding(note.getId()).isPresent(), is(false));
  }
}
