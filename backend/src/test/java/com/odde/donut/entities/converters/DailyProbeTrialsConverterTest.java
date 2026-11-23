package com.odde.donut.entities.converters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

class DailyProbeTrialsConverterTest {
  private final DailyProbeTrialsConverter converter = new DailyProbeTrialsConverter();
  private static final String STORED = "[{\"stimulus\":\"s\",\"rtMs\":5,\"correct\":true}]";

  @Test
  void storedTrialsRoundTripWithoutNulls() {
    assertThat(
        converter.convertToDatabaseColumn(converter.convertToEntityAttribute(STORED)), is(STORED));
  }
}
