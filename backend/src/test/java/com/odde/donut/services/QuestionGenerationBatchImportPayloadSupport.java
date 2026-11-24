package com.odde.donut.services;

import com.odde.donut.configs.ObjectMapperConfig;
import com.odde.donut.entities.QuestionGenerationBatchRequest;
import com.odde.donut.services.ai.GeneratedMcq;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

public final class QuestionGenerationBatchImportPayloadSupport {

  private static final JsonMapper OBJECT_MAPPER = ObjectMapperConfig.handWrittenJsonMapper();

  private QuestionGenerationBatchImportPayloadSupport() {}

  public static String batchSuccessLine(
      QuestionGenerationBatchRequest request, GeneratedMcq generatedMcq) {
    String fingerprint = FocusNoteFingerprint.of(request.getMemoryTracker().getNote());
    ObjectNode metadata =
        OBJECT_MAPPER.createObjectNode().put("focus_note_fingerprint", fingerprint);
    return batchSuccessLine(request, generatedMcq, metadata);
  }

  public static String batchSuccessLine(
      QuestionGenerationBatchRequest request, GeneratedMcq generatedMcq, JsonNode metadata) {
    String structuredOutput = OBJECT_MAPPER.writeValueAsString(generatedMcq);
    String responseBody =
        """
        {
          "id": "resp-1",
          "status": "completed",
          "output": [
            {
              "type": "message",
              "id": "msg-1",
              "status": "completed",
              "content": [
                {
                  "type": "output_text",
                  "text": %s
                }
              ]
            }
          ]
        }
        """
            .formatted(OBJECT_MAPPER.writeValueAsString(structuredOutput));
    ObjectNode body = OBJECT_MAPPER.readValue(responseBody, ObjectNode.class);
    if (metadata != null) {
      body.set("metadata", metadata);
    }

    return """
    {"id":"batch_req_1","custom_id":"%s","response":{"status_code":200,"body":%s},"error":null}\
    """
        .formatted(request.getCustomId(), body);
  }
}
