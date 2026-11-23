package com.odde.donut.services.openAiApis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.core.ObjectMappers;
import com.openai.models.responses.StructuredResponseCreateParams;
import java.lang.reflect.Method;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class StructuredResponseCreateParamsSerializer {
  static final String BODY_ACCESSOR_NAME = "_body";

  // SDK request bodies carry the SDK's Jackson 2 serializers, so only its own mapper writes them
  // faithfully.
  private static final ObjectMapper objectMapper = ObjectMappers.jsonMapper();

  public Map<String, Object> toBodyMap(StructuredResponseCreateParams<?> params) {
    try {
      Method bodyMethod = requireBodyAccessor(params.rawParams().getClass());
      Object body = bodyMethod.invoke(params.rawParams());
      String jsonString = objectMapper.writeValueAsString(body);
      Map<String, Object> result =
          objectMapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {});
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
}
