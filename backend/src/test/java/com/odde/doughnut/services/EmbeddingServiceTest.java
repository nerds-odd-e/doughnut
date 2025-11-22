package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odde.doughnut.entities.Note;
import com.openai.client.OpenAIClient;
import com.openai.models.embeddings.CreateEmbeddingResponse;
import com.openai.models.embeddings.Embedding;
import com.openai.models.embeddings.EmbeddingCreateParams;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class EmbeddingServiceTest {

  @Test
  void streamEmbeddingsForNoteList_shouldNotExceedApprox8000TokenBytesPerItem() {
    OpenAIClient officialClient = Mockito.mock(OpenAIClient.class);
    com.openai.services.blocking.EmbeddingService embeddingService =
        Mockito.mock(com.openai.services.blocking.EmbeddingService.class);
    when(officialClient.embeddings()).thenReturn(embeddingService);

    Embedding emb = Embedding.builder().index(0L).embedding(List.of(0.1f)).build();
    CreateEmbeddingResponse.Usage usage =
        CreateEmbeddingResponse.Usage.builder().promptTokens(0L).totalTokens(0L).build();
    CreateEmbeddingResponse response =
        CreateEmbeddingResponse.builder()
            .data(List.of(emb))
            .model("text-embedding-3-small")
            .usage(usage)
            .build();
    when(embeddingService.create(any(EmbeddingCreateParams.class))).thenReturn(response);

    EmbeddingService service = new EmbeddingService(officialClient);

    Note note = new Note();
    String longChunk = "你".repeat(10000) + "a".repeat(80000);
    note.setTopicConstructor("Title");
    note.setDetails(longChunk);

    // Trigger one batch call
    service.streamEmbeddingsForNoteList(List.of(note)).forEach(e -> {});

    ArgumentCaptor<EmbeddingCreateParams> captor =
        ArgumentCaptor.forClass(EmbeddingCreateParams.class);
    verify(embeddingService).create(captor.capture());
    EmbeddingCreateParams req = captor.getValue();

    List<String> inputs = req.input().arrayOfStrings().orElse(List.of());
    String sent = inputs.isEmpty() ? "" : inputs.get(0);

    int maxBytes = (int) Math.floor(8000 * 3.75);
    int sentBytes = sent.getBytes(StandardCharsets.UTF_8).length;
    assertThat(sentBytes, lessThanOrEqualTo(maxBytes));
  }
}
