package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.odde.doughnut.controllers.dto.SearchTerm;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.openai.client.OpenAIClient;
import com.openai.models.embeddings.CreateEmbeddingResponse;
import com.openai.models.embeddings.EmbeddingCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class SearchControllerSemanticTests extends ControllerTestBase {
  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  @Autowired SearchController controller;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
    // Default: return empty embedding data so semantic search falls back to literal search
    CreateEmbeddingResponse.Usage usage =
        CreateEmbeddingResponse.Usage.builder().promptTokens(0L).totalTokens(0L).build();
    CreateEmbeddingResponse empty =
        CreateEmbeddingResponse.builder()
            .data(java.util.List.of())
            .model("text-embedding-3-small")
            .usage(usage)
            .build();
    com.openai.services.blocking.EmbeddingService embeddingService =
        Mockito.mock(com.openai.services.blocking.EmbeddingService.class);
    when(officialClient.embeddings()).thenReturn(embeddingService);
    when(embeddingService.create(any(EmbeddingCreateParams.class))).thenReturn(empty);
  }

  @Nested
  class SemanticSearchWithinRelation {
    Note referenceNote;

    @BeforeEach
    void setup() {
      referenceNote =
          makeMe.aNote("Reference Note").notebookOwnedBy(currentUser.getUser()).please();
    }

    @Test
    void shouldReturnEmptyListWhenNoMatchingNotesInRelation()
        throws UnexpectedNoAccessRightException {
      var result = controller.semanticSearchWithin(referenceNote, searchTerm("nonexistent"));

      assertThat(result, empty());
    }

    @Test
    void shouldHandleEmptySearchKey() throws UnexpectedNoAccessRightException {
      var result = controller.semanticSearchWithin(referenceNote, searchTerm(""));

      assertThat(result, empty());
    }

    @Test
    void shouldHandleWhitespaceOnlySearchKey() throws UnexpectedNoAccessRightException {
      var result = controller.semanticSearchWithin(referenceNote, searchTerm("   "));

      assertThat(result, empty());
    }
  }

  @Nested
  class SearchTermValidationForSemanticEndpoints {
    Note referenceNote;

    @BeforeEach
    void setup() {
      referenceNote =
          makeMe.aNote("Reference Note").notebookOwnedBy(currentUser.getUser()).please();
    }

    @Test
    void shouldHandleNullSearchTerm() {
      assertThrows(IllegalArgumentException.class, () -> controller.semanticSearch(null));
    }

    @Test
    void shouldHandleNullSearchTermInRelation() {
      assertThrows(
          IllegalArgumentException.class,
          () -> controller.semanticSearchWithin(referenceNote, null));
    }
  }

  private static SearchTerm searchTerm(String searchKey) {
    SearchTerm searchTerm = new SearchTerm();
    searchTerm.setSearchKey(searchKey);
    searchTerm.setAllMyNotebooksAndSubscriptions(true);
    return searchTerm;
  }
}
