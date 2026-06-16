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
  static final String BODY_ACCESSOR_NAME = "_body";

  private final ObjectMapper objectMapper;

  public StructuredResponseCreateParamsSerializer(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public Map<String, Object> toBodyMap(StructuredResponseCreateParams<?> params) {
    try {
      Method bodyMethod = requireBodyAccessor(params.rawParams().getClass());
      Object body = bodyMethod.invoke(params.rawParams());
      String jsonString = objectMapper.writeValueAsString(body);
      Map<String, Object> result =
          objectMapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {});
      removeSyntheticValidFromStructuredOutputSchema(result);
      return result;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize StructuredResponseCreateParams", e);
    }
  }

  static Method requireBodyAccessor(Class<?> rawParamsClass) {
    try {
      return rawParamsClass.getMethod(BODY_ACCESSOR_NAME);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(
          "OpenAI SDK rawParams is missing expected "
              + BODY_ACCESSOR_NAME
              + "() accessor; StructuredResponseCreateParamsSerializer may need updating after an"
              + " SDK upgrade",
          e);
    }
  }

  void removeSyntheticValidFromStructuredOutputSchema(Map<String, Object> body) {
    removeShallowValidKey(body);
    Object reasoning = body.get("reasoning");
    if (reasoning instanceof Map<?, ?> reasoningMap) {
      @SuppressWarnings("unchecked")
      Map<String, Object> reasoningConfig = (Map<String, Object>) reasoningMap;
      removeShallowValidKey(reasoningConfig);
    }
    Object text = body.get("text");
    if (!(text instanceof Map<?, ?> textMap)) {
      return;
    }
    @SuppressWarnings("unchecked")
    Map<String, Object> textConfig = (Map<String, Object>) textMap;
    removeShallowValidKey(textConfig);
    Object format = textConfig.get("format");
    if (format != null) {
      removeValidFieldsRecursively(format);
    }
  }

  private static void removeShallowValidKey(Map<String, Object> map) {
    map.remove("valid");
  }

  @SuppressWarnings("unchecked")
  private void removeValidFieldsRecursively(Object obj) {
    if (obj == null) {
      return;
    }
    if (obj instanceof Map<?, ?> map) {
      Map<String, Object> stringKeyMap = (Map<String, Object>) map;
      stringKeyMap.remove("valid");
      for (Object value : stringKeyMap.values()) {
        removeValidFieldsRecursively(value);
      }
    } else if (obj instanceof List<?> list) {
      for (Object item : list) {
        removeValidFieldsRecursively(item);
      }
    }
  }
}
