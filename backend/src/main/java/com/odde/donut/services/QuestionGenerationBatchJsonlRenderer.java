package com.odde.donut.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.QuestionGenerationBatch;
import com.odde.donut.entities.QuestionGenerationBatchRequest;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.QuestionGenerationBatchRequestRepository;
import com.odde.donut.services.QuestionGenerationRequestBuilder.PreparedQuestionGeneration;
import com.odde.donut.services.openAiApis.StructuredResponseCreateParamsSerializer;
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
  static final String FOCUS_NOTE_FINGERPRINT_METADATA_KEY = "focus_note_fingerprint";

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
    PreparedQuestionGeneration prepared =
        requestBuilder.prepareBatchQuestionGeneration(
            tracker.getNote(), request.getContextSeed(), tracker.propertyFocus(), viewer);
    Map<String, Object> body = paramsSerializer.toBodyMap(prepared.request());
    body.put(
        "metadata", Map.of(FOCUS_NOTE_FINGERPRINT_METADATA_KEY, prepared.focusNoteFingerprint()));

    Map<String, Object> line = new LinkedHashMap<>();
    line.put("custom_id", request.getCustomId());
    line.put("method", BATCH_REQUEST_METHOD);
    line.put("url", BATCH_RESPONSES_ENDPOINT);
    line.put("body", body);

    try {
      return objectMapper.writeValueAsString(line);
    } catch (Exception e) {
      throw new RuntimeException("Failed to render batch JSONL line", e);
    }
  }
}
