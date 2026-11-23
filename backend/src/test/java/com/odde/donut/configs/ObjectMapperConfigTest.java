package com.odde.donut.configs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate7.Hibernate7Module;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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

  @Test
  void hibernateModuleMatchesJacksonCoreVersion() {
    var module = new Hibernate7Module().version();
    var core = objectMapper.version();

    assertThat(
        module.getMajorVersion() + "." + module.getMinorVersion(),
        is(core.getMajorVersion() + "." + core.getMinorVersion()));
  }

  private record TestObject(String field1, String field2, String field3) {}

  record Dated(
      String zeta, String alpha, String nothing, Instant at, Timestamp stamp, LocalDate day) {}

  static final Dated SAMPLE =
      new Dated(
          "z",
          "a",
          null,
          Instant.parse("2026-11-23T05:06:07Z"),
          Timestamp.from(Instant.parse("2026-11-23T05:06:07Z")),
          LocalDate.parse("2026-11-23"));

  interface HandWrittenJson {
    String write(Object value) throws Exception;

    <T> T read(String json, Class<T> type) throws Exception;
  }

  static Stream<Arguments> handWrittenMappers() {
    ObjectMapper jackson2 = new ObjectMapperConfig().objectMapper();
    tools.jackson.databind.json.JsonMapper jackson3 = ObjectMapperConfig.handWrittenJsonMapper();
    return Stream.of(
        Arguments.of(
            "jackson2",
            new HandWrittenJson() {
              public String write(Object value) throws Exception {
                return jackson2.writeValueAsString(value);
              }

              public <T> T read(String json, Class<T> type) throws Exception {
                return jackson2.readValue(json, type);
              }
            }),
        Arguments.of(
            "jackson3",
            new HandWrittenJson() {
              public String write(Object value) {
                return jackson3.writeValueAsString(value);
              }

              public <T> T read(String json, Class<T> type) {
                return jackson3.readValue(json, type);
              }
            }));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("handWrittenMappers")
  void writesHandWrittenJsonInDeclaredOrderWithoutNullsAndIsoDates(
      String name, HandWrittenJson json) throws Exception {
    assertThat(
        json.write(SAMPLE),
        is(
            "{\"zeta\":\"z\",\"alpha\":\"a\",\"at\":\"2026-11-23T05:06:07Z\","
                + "\"stamp\":\"2026-11-23T05:06:07.000+00:00\",\"day\":\"2026-11-23\"}"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("handWrittenMappers")
  void writesMapsWithoutNullEntries(String name, HandWrittenJson json) throws Exception {
    Map<String, Object> raw = new LinkedHashMap<>();
    raw.put("type", "text");
    raw.put("level", null);
    raw.put("page_idx", 3);

    assertThat(json.write(raw), is("{\"type\":\"text\",\"page_idx\":3}"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("handWrittenMappers")
  void readsBackWhatItWrote(String name, HandWrittenJson json) throws Exception {
    assertThat(json.read(json.write(SAMPLE), Dated.class), is(SAMPLE));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("handWrittenMappers")
  void refusesUnknownProperties(String name, HandWrittenJson json) {
    assertThrows(Exception.class, () -> json.read("{\"zeta\":\"z\",\"extra\":1}", Dated.class));
  }
}
