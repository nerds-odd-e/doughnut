package com.odde.doughnut.services.ai.tools;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class FunctionDefinitionTest {

  private final ObjectMapper objectMapper =
      new com.odde.doughnut.configs.ObjectMapperConfig().objectMapper();

  @Test
  void shouldSerializeWithStrictTrue() throws Exception {
    // given
    FunctionDefinition function =
        FunctionDefinition.builder()
            .name("test_function")
            .description("Test function")
            .strict(true)
            .build();

    // when
    String json = objectMapper.writeValueAsString(function);
    JsonNode jsonNode = objectMapper.readTree(json);

    // then
    assertThat(jsonNode.get("name").asText()).isEqualTo("test_function");
    assertThat(jsonNode.get("description").asText()).isEqualTo("Test function");
    assertThat(jsonNode.get("strict").asBoolean()).isTrue();
  }

  @Test
  void shouldSerializeWithStrictFalse() throws Exception {
    // given
    FunctionDefinition function =
        FunctionDefinition.builder()
            .name("test_function")
            .description("Test function")
            .strict(false)
            .build();

    // when
    String json = objectMapper.writeValueAsString(function);
    JsonNode jsonNode = objectMapper.readTree(json);

    // then
    assertThat(jsonNode.get("name").asText()).isEqualTo("test_function");
    assertThat(jsonNode.get("description").asText()).isEqualTo("Test function");
    assertThat(jsonNode.get("strict").asBoolean()).isFalse();
  }

  @Test
  void shouldSerializeWithoutStrict() throws Exception {
    // given
    FunctionDefinition function =
        FunctionDefinition.builder().name("test_function").description("Test function").build();

    // when
    String json = objectMapper.writeValueAsString(function);
    JsonNode jsonNode = objectMapper.readTree(json);

    // then
    assertThat(jsonNode.get("name").asText()).isEqualTo("test_function");
    assertThat(jsonNode.get("description").asText()).isEqualTo("Test function");
    assertThat(jsonNode.has("strict")).isFalse();
  }

  @Test
  void shouldSetAdditionalPropertiesFalseWhenStrictIsTrue() throws Exception {
    // given
    FunctionDefinition function =
        FunctionDefinition.<TestParameters>builder()
            .name("test_function")
            .description("Test function")
            .strict(true)
            .parametersDefinitionByClass(TestParameters.class)
            .build();

    // when
    String json = objectMapper.writeValueAsString(function);
    JsonNode jsonNode = objectMapper.readTree(json);
    JsonNode parametersNode = jsonNode.get("parameters");

    // then
    assertThat(parametersNode.get("additionalProperties").asBoolean()).isFalse();
  }

  @Test
  void whenStrictIsFalse() throws Exception {
    // given
    FunctionDefinition function =
        FunctionDefinition.<TestParameters>builder()
            .name("test_function")
            .description("Test function")
            .strict(false)
            .parametersDefinitionByClass(TestParameters.class)
            .build();

    // when
    String json = objectMapper.writeValueAsString(function);
    JsonNode jsonNode = objectMapper.readTree(json);
    JsonNode parametersNode = jsonNode.get("parameters");

    // then
    assertThat(parametersNode.has("additionalProperties")).isFalse();
  }

  @Test
  void shouldNotSetAdditionalPropertiesWhenStrictIsNull() throws Exception {
    // given
    FunctionDefinition function =
        FunctionDefinition.<TestParameters>builder()
            .name("test_function")
            .description("Test function")
            .parametersDefinitionByClass(TestParameters.class)
            .build();

    // when
    String json = objectMapper.writeValueAsString(function);
    JsonNode jsonNode = objectMapper.readTree(json);
    JsonNode parametersNode = jsonNode.get("parameters");

    // then
    assertThat(parametersNode.has("additionalProperties")).isFalse();
  }
}
