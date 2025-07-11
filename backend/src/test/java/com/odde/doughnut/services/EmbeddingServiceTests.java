package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.testability.MakeMe;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.embedding.Embedding;
import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.embedding.EmbeddingResult;
import io.reactivex.Single;
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
class EmbeddingServiceTests {

  @Mock OpenAiApi openAiApi;
  @Autowired MakeMe makeMe;

  EmbeddingService service;

  @BeforeEach
  void setup() {
    service = new EmbeddingService(openAiApi);
  }

  @Test
  void shouldGenerateEmbeddingForNote() {
    Note note = makeMe.aNote().titleConstructor("Test Title").details("Test details").please();

    Embedding embedding = new Embedding();
    embedding.setEmbedding(List.of(1.0f, 2.0f, 3.0f));

    EmbeddingResult result = new EmbeddingResult();
    result.setData(List.of(embedding));

    when(openAiApi.createEmbeddings(any(EmbeddingRequest.class))).thenReturn(Single.just(result));

    Optional<List<Float>> resultEmbedding = service.generateEmbedding(note);

    assertThat(resultEmbedding.isPresent(), is(true));
    assertThat(resultEmbedding.get(), equalTo(List.of(1.0f, 2.0f, 3.0f)));
  }

  @Test
  void shouldReturnEmptyWhenNoEmbeddingData() {
    Note note = makeMe.aNote().please();

    EmbeddingResult result = new EmbeddingResult();
    result.setData(List.of()); // Empty result

    when(openAiApi.createEmbeddings(any(EmbeddingRequest.class))).thenReturn(Single.just(result));

    Optional<List<Float>> resultEmbedding = service.generateEmbedding(note);

    assertThat(resultEmbedding.isPresent(), is(false));
  }
}
