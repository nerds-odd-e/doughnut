package com.odde.donut.services;

import com.odde.donut.configs.ObjectMapperConfig;
import com.odde.donut.entities.FocusNoteInput;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.QuestionGenerationBatch;
import com.odde.donut.entities.QuestionGenerationBatchRequest;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.QuestionGenerationBatchRequestRepository;
import com.odde.donut.services.QuestionGenerationRequestBuilder.PreparedQuestionGeneration;
import com.odde.donut.services.openAiApis.StructuredResponseCreateParamsSerializer;
import java.sql.Timestamp;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class QuestionGenerationBatchJsonlRenderer {
  public static final String BATCH_RESPONSES_ENDPOINT = "/v1/responses";
  public static final String BATCH_REQUEST_METHOD = "POST";
  private static final String FOCUS_NOTE_FINGERPRINT_METADATA_KEY = "focus_note_fingerprint";
  private static final String FOCUS_NOTE_ID_METADATA_KEY = "focus_note_id";
  private static final String FOCUS_NOTE_READ_AT_METADATA_KEY = "focus_note_read_at";

  private final QuestionGenerationBatchRequestRepository batchRequestRepository;
  private final QuestionGenerationRequestBuilder requestBuilder;
  private static final JsonMapper objectMapper = ObjectMapperConfig.handWrittenJsonMapper();
  private final StructuredResponseCreateParamsSerializer paramsSerializer;

  @Autowired
  public QuestionGenerationBatchJsonlRenderer(
      QuestionGenerationBatchRequestRepository batchRequestRepository,
      QuestionGenerationRequestBuilder requestBuilder,
      StructuredResponseCreateParamsSerializer paramsSerializer) {
    this.batchRequestRepository = batchRequestRepository;
    this.requestBuilder = requestBuilder;
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
    body.put("metadata", focusNoteInputMetadata(prepared.focusNoteInput()));

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

  private static Map<String, String> focusNoteInputMetadata(FocusNoteInput input) {
    return Map.of(
        FOCUS_NOTE_FINGERPRINT_METADATA_KEY, input.fingerprint(),
        FOCUS_NOTE_ID_METADATA_KEY, String.valueOf(input.noteId()),
        FOCUS_NOTE_READ_AT_METADATA_KEY, String.valueOf(input.readAt().getTime()));
  }

  /** Batch rows submitted before the id and read time were recorded carry only the fingerprint. */
  static FocusNoteInput focusNoteInputFrom(JsonNode metadata) {
    String fingerprint = metadata.path(FOCUS_NOTE_FINGERPRINT_METADATA_KEY).stringValue(null);
    if (!FocusNoteInput.isValidFingerprint(fingerprint)) {
      return null;
    }
    JsonNode noteId = metadata.path(FOCUS_NOTE_ID_METADATA_KEY);
    JsonNode readAt = metadata.path(FOCUS_NOTE_READ_AT_METADATA_KEY);
    return new FocusNoteInput(
        noteId.isMissingNode() ? null : Integer.valueOf(noteId.stringValue(null)),
        readAt.isMissingNode() ? null : new Timestamp(Long.parseLong(readAt.stringValue(null))),
        fingerprint);
  }
}
