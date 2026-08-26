package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.openai.client.OpenAIClient;
import com.openai.models.embeddings.CreateEmbeddingResponse;
import com.openai.models.embeddings.EmbeddingCreateParams;
import com.openai.services.blocking.EmbeddingService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class SearchControllerSemanticTests extends SearchControllerTestBase {
  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  Note referenceNote;

  @BeforeEach
  void setupSemantic() {
    CreateEmbeddingResponse.Usage usage =
        CreateEmbeddingResponse.Usage.builder().promptTokens(0L).totalTokens(0L).build();
    CreateEmbeddingResponse empty =
        CreateEmbeddingResponse.builder()
            .data(List.of())
            .model("text-embedding-3-small")
            .usage(usage)
            .build();
    EmbeddingService embeddingService = Mockito.mock(EmbeddingService.class);
    when(officialClient.embeddings()).thenReturn(embeddingService);
    when(embeddingService.create(any(EmbeddingCreateParams.class))).thenReturn(empty);

    referenceNote = ownedNote("Reference Note");
  }

  @Test
  void shouldReturnEmptyListWhenNoMatchingNotesInRelation()
      throws UnexpectedNoAccessRightException {
    assertThat(controller.semanticSearchWithin(referenceNote, searchTerm("nonexistent")), empty());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  void shouldHandleBlankSearchKey(String searchKey) throws UnexpectedNoAccessRightException {
    assertThat(controller.semanticSearchWithin(referenceNote, searchTerm(searchKey)), empty());
  }

  @Test
  void shouldHandleNullSearchTerm() {
    assertThrows(IllegalArgumentException.class, () -> controller.semanticSearch(null));
  }

  @Test
  void shouldHandleNullSearchTermInRelation() {
    assertThrows(
        IllegalArgumentException.class, () -> controller.semanticSearchWithin(referenceNote, null));
  }
}
