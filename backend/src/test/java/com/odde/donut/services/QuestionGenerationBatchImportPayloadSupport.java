package com.odde.donut.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.configs.ObjectMapperConfig;
import com.odde.donut.services.ai.GeneratedMcq;

final class QuestionGenerationBatchImportPayloadSupport {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperConfig().objectMapper();

  private QuestionGenerationBatchImportPayloadSupport() {}

  static String batchSuccessLine(String customId, GeneratedMcq generatedMcq)
      throws JsonProcessingException {
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

    return """
        {"id":"batch_req_1","custom_id":"%s","response":{"status_code":200,"body":%s},"error":null}"""
        .formatted(customId, responseBody);
  }
}
