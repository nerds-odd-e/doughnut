package com.odde.donut.configs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ObjectMapperConfigTest {
  private final JsonMapper json = ObjectMapperConfig.handWrittenJsonMapper();

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

  @Test
  void writesHandWrittenJsonInDeclaredOrderWithoutNullsAndIsoDates() {
    assertThat(
        json.writeValueAsString(SAMPLE),
        is(
            "{\"zeta\":\"z\",\"alpha\":\"a\",\"at\":\"2026-11-23T05:06:07Z\","
                + "\"stamp\":\"2026-11-23T05:06:07.000+00:00\",\"day\":\"2026-11-23\"}"));
  }

  @Test
  void writesMapsWithoutNullEntries() {
    Map<String, Object> raw = new LinkedHashMap<>();
    raw.put("type", "text");
    raw.put("level", null);
    raw.put("page_idx", 3);

    assertThat(json.writeValueAsString(raw), is("{\"type\":\"text\",\"page_idx\":3}"));
  }

  @Test
  void readsBackWhatItWrote() {
    assertThat(json.readValue(json.writeValueAsString(SAMPLE), Dated.class), is(SAMPLE));
  }

  @Test
  void refusesUnknownProperties() {
    assertThrows(
        Exception.class, () -> json.readValue("{\"zeta\":\"z\",\"extra\":1}", Dated.class));
  }
}
