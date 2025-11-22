package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.testability.MakeMe;
import com.openai.client.OpenAIClient;
import com.openai.models.embeddings.CreateEmbeddingResponse;
import com.openai.models.embeddings.Embedding;
import com.openai.models.embeddings.EmbeddingCreateParams;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EmbeddingServiceTests {

  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  @Autowired MakeMe makeMe;
  @Autowired com.odde.doughnut.services.EmbeddingService service;

  @Test
  void shouldStreamEmbeddingsForNotes() {
    // Build a small ancestor chain to verify separator rendering
    Note root = makeMe.aNote().titleConstructor("Root").please();
    Note parent = makeMe.aNote().under(root).titleConstructor("Parent").please();
    Note note1 = makeMe.aNote().under(parent).titleConstructor("T1").details("D1").please();
    Note note2 = makeMe.aNote().titleConstructor("T2").details("D2").please();

    Embedding embedding1 = Embedding.builder().index(0L).embedding(List.of(1.0f, 2.0f)).build();
    Embedding embedding2 = Embedding.builder().index(1L).embedding(List.of(3.0f, 4.0f)).build();

    CreateEmbeddingResponse.Usage usage =
        CreateEmbeddingResponse.Usage.builder().promptTokens(0L).totalTokens(0L).build();
    CreateEmbeddingResponse response =
        CreateEmbeddingResponse.builder()
            .data(List.of(embedding1, embedding2))
            .model("text-embedding-3-small")
            .usage(usage)
            .build();

    com.openai.services.blocking.EmbeddingService embeddingService =
        Mockito.mock(com.openai.services.blocking.EmbeddingService.class);
    when(officialClient.embeddings()).thenReturn(embeddingService);
    when(embeddingService.create(any(EmbeddingCreateParams.class)))
        .thenAnswer(
            invocation -> {
              EmbeddingCreateParams params = invocation.getArgument(0);
              // Ensure the input contains our structured fields and the "›" separator
              List<String> inputs = params.input().arrayOfStrings().orElse(List.of());
              String first = inputs.get(0);
              org.junit.jupiter.api.Assertions.assertTrue(first.contains("Context:"));
              org.junit.jupiter.api.Assertions.assertTrue(first.contains("Root \u203A Parent"));
              org.junit.jupiter.api.Assertions.assertTrue(first.contains("Title: T1"));
              org.junit.jupiter.api.Assertions.assertTrue(first.contains("Details:"));
              return response;
            });

    var streamed = service.streamEmbeddingsForNoteList(List.of(note1, note2)).toList();

    assertThat(streamed.size(), equalTo(2));
    assertThat(streamed.get(0).note(), equalTo(note1));
    assertThat(streamed.get(0).embedding().get(), equalTo(List.of(1.0f, 2.0f)));
    assertThat(streamed.get(1).note(), equalTo(note2));
    assertThat(streamed.get(1).embedding().get(), equalTo(List.of(3.0f, 4.0f)));
  }

  @Test
  void shouldEmitEmptyEmbeddingsWhenNoEmbeddingData() {
    Note note1 = makeMe.aNote().please();
    Note note2 = makeMe.aNote().please();

    CreateEmbeddingResponse.Usage usage =
        CreateEmbeddingResponse.Usage.builder().promptTokens(0L).totalTokens(0L).build();
    CreateEmbeddingResponse response =
        CreateEmbeddingResponse.builder()
            .data(List.of()) // Empty result
            .model("text-embedding-3-small")
            .usage(usage)
            .build();

    com.openai.services.blocking.EmbeddingService embeddingService =
        Mockito.mock(com.openai.services.blocking.EmbeddingService.class);
    when(officialClient.embeddings()).thenReturn(embeddingService);
    when(embeddingService.create(any(EmbeddingCreateParams.class))).thenReturn(response);

    var streamed = service.streamEmbeddingsForNoteList(List.of(note1, note2)).toList();

    assertThat(streamed.size(), equalTo(2));
    assertThat(streamed.get(0).embedding().isPresent(), is(false));
    assertThat(streamed.get(1).embedding().isPresent(), is(false));
  }
}
