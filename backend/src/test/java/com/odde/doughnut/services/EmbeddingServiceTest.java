package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odde.doughnut.entities.Note;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.embedding.Embedding;
import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.embedding.EmbeddingResult;
import io.reactivex.Single;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EmbeddingServiceTest {

  @Test
  void streamEmbeddingsForNoteList_shouldNotExceedApprox8000TokenBytesPerItem() {
    OpenAiApi api = org.mockito.Mockito.mock(OpenAiApi.class);
    EmbeddingResult result = new EmbeddingResult();
    Embedding emb = new Embedding();
    emb.setEmbedding(List.of(0.1f));
    result.setData(List.of(emb));
    when(api.createEmbeddings(any(EmbeddingRequest.class))).thenReturn(Single.just(result));

    EmbeddingService service = new EmbeddingService(api);

    Note note = new Note();
    String longChunk = "你".repeat(10000) + "a".repeat(80000);
    note.setTopicConstructor("Title");
    note.setDetails(longChunk);

    // Trigger one batch call
    service.streamEmbeddingsForNoteList(List.of(note)).forEach(e -> {});

    ArgumentCaptor<EmbeddingRequest> captor = ArgumentCaptor.forClass(EmbeddingRequest.class);
    verify(api).createEmbeddings(captor.capture());
    EmbeddingRequest req = captor.getValue();

    Object input = req.getInput();
    String sent;
    if (input instanceof List<?> list && !list.isEmpty()) {
      Object first = list.get(0);
      sent = String.valueOf(first);
    } else {
      sent = String.valueOf(input);
    }

    int maxBytes = (int) Math.floor(8000 * 3.75);
    int sentBytes = sent.getBytes(StandardCharsets.UTF_8).length;
    assertThat(sentBytes, lessThanOrEqualTo(maxBytes));
  }
}
