package com.odde.doughnut.services;

import static com.odde.doughnut.services.openAiApis.ApiExecutor.blockGet;

import com.odde.doughnut.entities.Note;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.embedding.EmbeddingResult;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {
  private final OpenAiApi openAiApi;

  public EmbeddingService(@Qualifier("testableOpenAiApi") OpenAiApi openAiApi) {
    this.openAiApi = openAiApi;
  }

  public Optional<List<Float>> generateEmbedding(Note note) {
    String content = combineNoteContent(note);
    return generateEmbedding(content);
  }

  public Optional<List<Float>> generateEmbedding(String content) {
    EmbeddingRequest request =
        EmbeddingRequest.builder().model("text-embedding-3-small").input(List.of(content)).build();

    EmbeddingResult result = blockGet(openAiApi.createEmbeddings(request));

    if (result != null && result.getData() != null && !result.getData().isEmpty()) {
      // The OpenAI Java client returns List<Float>
      @SuppressWarnings("unchecked")
      List<Float> embedding = (List<Float>) result.getData().get(0).getEmbedding();
      return Optional.of(embedding);
    }
    return Optional.empty();
  }

  private String combineNoteContent(Note note) {
    String title = note.getTopicConstructor() != null ? note.getTopicConstructor() : "";
    String details = note.getDetails() != null ? note.getDetails() : "";
    return title + " " + details;
  }
}
