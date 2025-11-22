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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EmbeddingServiceTests {

  @MockitoBean(name = "testableOpenAiApi")
  OpenAiApi openAiApi;

  @Autowired MakeMe makeMe;
  @Autowired EmbeddingService service;

  @Test
  void shouldStreamEmbeddingsForNotes() {
    // Build a small ancestor chain to verify separator rendering
    Note root = makeMe.aNote().titleConstructor("Root").please();
    Note parent = makeMe.aNote().under(root).titleConstructor("Parent").please();
    Note note1 = makeMe.aNote().under(parent).titleConstructor("T1").details("D1").please();
    Note note2 = makeMe.aNote().titleConstructor("T2").details("D2").please();

    Embedding embedding1 = new Embedding();
    embedding1.setEmbedding(List.of(1.0f, 2.0f));
    Embedding embedding2 = new Embedding();
    embedding2.setEmbedding(List.of(3.0f, 4.0f));

    EmbeddingResult result = new EmbeddingResult();
    result.setData(List.of(embedding1, embedding2));

    when(openAiApi.createEmbeddings(any(EmbeddingRequest.class)))
        .thenAnswer(
            invocation -> {
              EmbeddingRequest req = invocation.getArgument(0);
              // Ensure the input contains our structured fields and the "›" separator
              Object input = req.getInput();
              java.util.List<?> list = (java.util.List<?>) input;
              String first = String.valueOf(list.get(0));
              org.junit.jupiter.api.Assertions.assertTrue(first.contains("Context:"));
              org.junit.jupiter.api.Assertions.assertTrue(first.contains("Root \u203A Parent"));
              org.junit.jupiter.api.Assertions.assertTrue(first.contains("Title: T1"));
              org.junit.jupiter.api.Assertions.assertTrue(first.contains("Details:"));
              return Single.just(result);
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

    EmbeddingResult result = new EmbeddingResult();
    result.setData(List.of()); // Empty result

    when(openAiApi.createEmbeddings(any(EmbeddingRequest.class))).thenReturn(Single.just(result));

    var streamed = service.streamEmbeddingsForNoteList(List.of(note1, note2)).toList();

    assertThat(streamed.size(), equalTo(2));
    assertThat(streamed.get(0).embedding().isPresent(), is(false));
    assertThat(streamed.get(1).embedding().isPresent(), is(false));
  }
}
