package com.odde.donut.services;

import com.openai.client.OpenAIClient;
import com.openai.models.embeddings.CreateEmbeddingResponse;
import com.openai.models.embeddings.Embedding;
import com.openai.models.embeddings.EmbeddingCreateParams;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {
  private final OpenAIClient officialClient;
  private static final int MAX_TOKENS_PER_INPUT = 4000; // per-item token cap
  private static final String EMBEDDING_MODEL = "text-embedding-3-small";

  public EmbeddingService(@Qualifier("officialOpenAiClient") OpenAIClient officialClient) {
    this.officialClient = officialClient;
  }

  /** Generate an embedding vector for a free-form search query. */
  public List<Float> generateQueryEmbedding(String query) {
    String input =
        ApproximateUtf8TokenBudget.truncateByApproxTokens(
            query == null ? "" : query.trim(), MAX_TOKENS_PER_INPUT);
    EmbeddingCreateParams params =
        EmbeddingCreateParams.builder().model(EMBEDDING_MODEL).input(input).build();
    CreateEmbeddingResponse response = officialClient.embeddings().create(params);
    if (response != null && response.data() != null && !response.data().isEmpty()) {
      Embedding embedding = response.data().get(0);
      return embedding.embedding();
    }
    return List.of();
  }
}
