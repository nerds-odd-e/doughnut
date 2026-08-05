package com.odde.doughnut.configs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ObjectMapperConfigTest {
  private final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();

  @Test
  void shouldExcludeNullFieldsFromJsonSerialization() throws JsonProcessingException {
    String jsonString = objectMapper.writeValueAsString(new TestObject("value1", null, "value3"));
    JsonNode jsonNode = objectMapper.readTree(jsonString);

    assertThat(jsonNode.get("field1").asText(), is("value1"));
    assertThat(jsonNode.has("field2"), is(false));
    assertThat(jsonNode.get("field3").asText(), is("value3"));
  }

  private record TestObject(String field1, String field2, String field3) {}
}
