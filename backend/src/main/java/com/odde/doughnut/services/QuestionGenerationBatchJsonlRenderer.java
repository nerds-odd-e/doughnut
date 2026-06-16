package com.odde.doughnut.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.QuestionGenerationBatch;
import com.odde.doughnut.entities.QuestionGenerationBatchRequest;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRequestRepository;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.openAiApis.StructuredResponseCreateParamsSerializer;
import com.openai.models.responses.StructuredResponseCreateParams;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QuestionGenerationBatchJsonlRenderer {
  public static final String BATCH_RESPONSES_ENDPOINT = "/v1/responses";
  public static final String BATCH_REQUEST_METHOD = "POST";

  private final QuestionGenerationBatchRequestRepository batchRequestRepository;
  private final QuestionGenerationRequestBuilder requestBuilder;
  private final ObjectMapper objectMapper;
  private final StructuredResponseCreateParamsSerializer paramsSerializer;

  @Autowired
  public QuestionGenerationBatchJsonlRenderer(
      QuestionGenerationBatchRequestRepository batchRequestRepository,
      QuestionGenerationRequestBuilder requestBuilder,
      ObjectMapper objectMapper,
      StructuredResponseCreateParamsSerializer paramsSerializer) {
    this.batchRequestRepository = batchRequestRepository;
    this.requestBuilder = requestBuilder;
    this.objectMapper = objectMapper;
    this.paramsSerializer = paramsSerializer;
  }

  public String renderInputJsonl(QuestionGenerationBatch batch) {
    User viewer = batch.getUser();
    List<QuestionGenerationBatchRequest> requests =
        batchRequestRepository.findByBatch_Id(batch.getId()).stream()
            .sorted(Comparator.comparing(QuestionGenerationBatchRequest::getId))
            .toList();

    StringBuilder jsonl = new StringBuilder();
    for (QuestionGenerationBatchRequest request : requests) {
      if (!jsonl.isEmpty()) {
        jsonl.append('\n');
      }
      jsonl.append(renderLine(viewer, request));
    }
    return jsonl.toString();
  }

  private String renderLine(User viewer, QuestionGenerationBatchRequest request) {
    MemoryTracker tracker = request.getMemoryTracker();
    StructuredResponseCreateParams<MCQWithAnswer> params =
        requestBuilder.buildQuestionGenerationResponseRequest(
            tracker.getNote(),
            null,
            request.getContextSeed(),
            propertyKeyOrNull(tracker.getPropertyKey()),
            viewer);

    Map<String, Object> line = new LinkedHashMap<>();
    line.put("custom_id", request.getCustomId());
    line.put("method", BATCH_REQUEST_METHOD);
    line.put("url", BATCH_RESPONSES_ENDPOINT);
    line.put("body", paramsSerializer.toBodyMap(params));

    try {
      return objectMapper.writeValueAsString(line);
    } catch (Exception e) {
      throw new RuntimeException("Failed to render batch JSONL line", e);
    }
  }

  private static String propertyKeyOrNull(String propertyKey) {
    if (propertyKey == null || propertyKey.isBlank()) {
      return null;
    }
    return propertyKey;
  }
}
