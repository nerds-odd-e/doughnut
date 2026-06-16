package com.odde.doughnut.services.openAiApis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.QuestionGenerationRequestBuilder;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.testability.MakeMe;
import com.openai.models.responses.StructuredResponseCreateParams;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StructuredResponseCreateParamsSerializerTest {

  @Autowired MakeMe makeMe;
  @Autowired QuestionGenerationRequestBuilder requestBuilder;
  @Autowired GlobalSettingsService globalSettingsService;
  @Autowired StructuredResponseCreateParamsSerializer paramsSerializer;

  @TestBean CurrentUser currentUser;

  private final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();
  private User user;

  static CurrentUser currentUser() {
    return new CurrentUser();
  }

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    currentUser.setUser(user);
    Timestamp currentTime = makeMe.aTimestamp().please();
    globalSettingsService
        .globalSettingQuestionGeneration()
        .setKeyValue(currentTime, "gpt-batch-question-generation");
  }

  private StructuredResponseCreateParams<MCQWithAnswer> sampleParams() {
    Note note =
        makeMe.aNote().notebookOwnedBy(user).title("Tokyo is the capital of Japan").please();
    return requestBuilder.buildQuestionGenerationResponseRequest(note, null, null, null);
  }

  private List<String> findValidFieldPaths(Object obj) {
    List<String> validFields = new ArrayList<>();
    findValidFieldPathsRecursive(obj, "", validFields);
    return validFields;
  }

  @SuppressWarnings("unchecked")
  private void findValidFieldPathsRecursive(Object obj, String path, List<String> validFields) {
    if (obj == null) {
      return;
    }
    if (obj instanceof Map<?, ?> map) {
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        String key = String.valueOf(entry.getKey());
        Object value = entry.getValue();
        String currentPath = path.isEmpty() ? key : path + "." + key;
        if ("valid".equals(key)) {
          validFields.add(currentPath);
        }
        findValidFieldPathsRecursive(value, currentPath, validFields);
      }
    } else if (obj instanceof List<?> list) {
      for (int i = 0; i < list.size(); i++) {
        findValidFieldPathsRecursive(list.get(i), path + "[" + i + "]", validFields);
      }
    }
  }

  @Nested
  class ToBodyMap {
    @Test
    void rendersResponsesApiBodyShapeWithoutSyntheticValidField() throws Exception {
      StructuredResponseCreateParams<MCQWithAnswer> params = sampleParams();

      Map<String, Object> body = paramsSerializer.toBodyMap(params);

      assertThat(body.get("model"), is("gpt-batch-question-generation"));
      assertThat(body.get("max_output_tokens"), is(1000));
      assertThat(body.containsKey("instructions"), is(true));
      assertThat(body.containsKey("input"), is(true));
      assertThat(body.containsKey("text"), is(true));

      @SuppressWarnings("unchecked")
      Map<String, Object> text = (Map<String, Object>) body.get("text");
      assertThat(text.containsKey("format"), is(true));
      @SuppressWarnings("unchecked")
      Map<String, Object> format = (Map<String, Object>) text.get("format");
      assertThat(format.get("type"), is("json_schema"));
      assertThat(format.containsKey("schema"), is(true));

      assertThat(
          "body should not contain synthetic 'valid' fields from MCQWithAnswer.isValid()",
          findValidFieldPaths(body),
          empty());
    }

    @Test
    void failsClearlyWhenOpenAiSdkBodyAccessorIsUnavailable() {
      RuntimeException error =
          assertThrows(
              RuntimeException.class,
              () -> StructuredResponseCreateParamsSerializer.requireBodyAccessor(Object.class));

      assertThat(
          error.getMessage(),
          containsString(StructuredResponseCreateParamsSerializer.BODY_ACCESSOR_NAME));
      assertThat(error.getMessage(), containsString("SDK"));
      assertThat(error.getCause(), instanceOf(NoSuchMethodException.class));
    }
  }

  @Nested
  class RemoveSyntheticValidFromStructuredOutputSchema {
    @Test
    void removesValidOnlyWithinStructuredOutputSchema() throws Exception {
      StructuredResponseCreateParams<MCQWithAnswer> params = sampleParams();
      Method bodyMethod =
          params
              .rawParams()
              .getClass()
              .getMethod(StructuredResponseCreateParamsSerializer.BODY_ACCESSOR_NAME);
      String jsonString = objectMapper.writeValueAsString(bodyMethod.invoke(params.rawParams()));
      Map<String, Object> body =
          objectMapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {});

      Map<String, Object> metadata = new LinkedHashMap<>();
      metadata.put("valid", true);
      body.put("metadata", metadata);

      paramsSerializer.removeSyntheticValidFromStructuredOutputSchema(body);

      @SuppressWarnings("unchecked")
      Map<String, Object> preservedMetadata = (Map<String, Object>) body.get("metadata");
      assertThat(preservedMetadata.get("valid"), is(true));
      assertThat(body.containsKey("valid"), is(false));

      @SuppressWarnings("unchecked")
      Map<String, Object> text = (Map<String, Object>) body.get("text");
      @SuppressWarnings("unchecked")
      Map<String, Object> format = (Map<String, Object>) text.get("format");
      assertThat(findValidFieldPaths(format), empty());
    }

    @Test
    void preservesValidOutsideStructuredOutputConfig() {
      Map<String, Object> body = new LinkedHashMap<>();
      body.put("model", "gpt-test");
      Map<String, Object> metadata = new LinkedHashMap<>();
      metadata.put("valid", true);
      body.put("metadata", metadata);

      paramsSerializer.removeSyntheticValidFromStructuredOutputSchema(body);

      @SuppressWarnings("unchecked")
      Map<String, Object> preservedMetadata = (Map<String, Object>) body.get("metadata");
      assertThat(preservedMetadata.get("valid"), is(true));
      assertThat(body.get("model"), is("gpt-test"));
    }
  }
}
