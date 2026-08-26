package com.odde.donut.services.search;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.odde.donut.controllers.dto.SearchTerm;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NoteEmbeddingJdbcRepository;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.services.EmbeddingService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SemanticNoteSearchServiceEmptyEmbeddingTest {

  EmbeddingService embeddingService;
  SemanticNoteSearchService semanticNoteSearchService;

  @BeforeEach
  void setup() {
    embeddingService = mock(EmbeddingService.class);
    semanticNoteSearchService =
        new SemanticNoteSearchService(
            mock(NoteRepository.class), mock(NoteEmbeddingJdbcRepository.class), embeddingService);
  }

  @Test
  void returnsEmptyListWhenQueryEmbeddingIsEmpty() {
    User user = new User();
    SearchTerm searchTerm = new SearchTerm();
    searchTerm.setSearchKey("nonexistent");
    when(embeddingService.generateQueryEmbedding("nonexistent")).thenReturn(List.of());

    assertThat(semanticNoteSearchService.semanticSearchForNotes(user, searchTerm), empty());
  }
}
