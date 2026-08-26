package com.odde.donut.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TimestampOperationsTest {

  @ParameterizedTest
  @CsvSource({
    "2020-01-01T00:00:00Z, 01 January 2020",
    "1990-12-25T12:30:45Z, 25 December 1990",
    "-0044-03-15T00:00:00Z, 15 March 0045 B.C.",
    "1170-01-01T00:00:00Z, 01 January 1170",
    "1170-00-01T00:00:00Z, 01 January 1170",
    "1170-01-00T00:00:00Z, January 1170",
    "1170-00-00T00:00:00Z, 1170",
    "-1170-00-00T00:00:00Z, 1171 B.C.",
    "-1170-10-00T00:00:00Z, October 1171 B.C.",
    "+1980-03-31T00:00:00Z, 31 March 1980",
    "-0552-10-09T00:00:00Z, 09 October 0553 B.C."
  })
  void formatISOTimeToYearSupportingBC(String input, String expected) {
    assertThat(TimestampOperations.formatISOTimeToYearSupportingBC(input), equalTo(expected));
  }
}
