package com.odde.doughnut.services.openAiApis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.models.responses.StructuredResponseCreateParams;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class StructuredResponseCreateParamsSerializer {
  private final ObjectMapper objectMapper;

  public StructuredResponseCreateParamsSerializer(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public Map<String, Object> toBodyMap(StructuredResponseCreateParams<?> params) {
    try {
      Method bodyMethod = params.rawParams().getClass().getMethod("_body");
      Object body = bodyMethod.invoke(params.rawParams());
      String jsonString = objectMapper.writeValueAsString(body);
      Map<String, Object> result =
          objectMapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {});
      removeValidFields(result);
      return result;
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize StructuredResponseCreateParams", e);
    }
  }

  @SuppressWarnings("unchecked")
  private void removeValidFields(Object obj) {
    if (obj == null) {
      return;
    }
    if (obj instanceof Map<?, ?> map) {
      Map<String, Object> stringKeyMap = (Map<String, Object>) map;
      stringKeyMap.remove("valid");
      for (Object value : stringKeyMap.values()) {
        removeValidFields(value);
      }
    } else if (obj instanceof List<?> list) {
      for (Object item : list) {
        removeValidFields(item);
      }
    }
  }
}
