package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.configs.ObjectMapperConfig;

public class ChatMessageContent {

  private static final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();

  public static String extractContentString(Object contentObj) {
    if (contentObj == null) {
      return null;
    }
    if (contentObj instanceof String) {
      return (String) contentObj;
    }
    try {
      com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.valueToTree(contentObj);
      if (jsonNode.isTextual()) {
        return jsonNode.asText();
      }
      if (jsonNode.isObject()) {
        if (jsonNode.has("text")) {
          return jsonNode.get("text").asText();
        }
        if (jsonNode.has("content")) {
          return jsonNode.get("content").asText();
        }
      }
      String json = objectMapper.writeValueAsString(contentObj);
      if (json.startsWith("\"") && json.endsWith("\"")) {
        return json.substring(1, json.length() - 1);
      }
      return json;
    } catch (Exception e) {
      String str = contentObj.toString();
      if (str.startsWith("\"") && str.endsWith("\"")) {
        return str.substring(1, str.length() - 1);
      }
      return str;
    }
  }
}
