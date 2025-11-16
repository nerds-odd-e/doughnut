package com.odde.doughnut.models;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.doughnut.utils.TimestampOperations;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TimestampOperationsTest {

  @ParameterizedTest
  @CsvSource({
    "2020-01-01T00:00:00Z, 01 January 2020",
    "1990-12-25T12:30:45Z, 25 December 1990",
    "-0044-03-15T00:00:00Z, 15 March 0045 B.C.",
    "1170-01-01T00:00:00Z, 01 January 1170"
  })
  void formatISOTimeToYearSupportingBC_validDates(String input, String expected) {
    assertThat(TimestampOperations.formatISOTimeToYearSupportingBC(input), equalTo(expected));
  }

  @Test
  void formatISOTimeToYearSupportingBC_handlesInvalidMonth() {
    // Invalid month "00" should be replaced with "01"
    String result = TimestampOperations.formatISOTimeToYearSupportingBC("1170-00-01T00:00:00Z");
    assertThat(result, equalTo("01 January 1170"));
  }

  @Test
  void formatISOTimeToYearSupportingBC_handlesInvalidDay() {
    // Invalid day "00" should return just the month and year
    String result = TimestampOperations.formatISOTimeToYearSupportingBC("1170-01-00T00:00:00Z");
    assertThat(result, equalTo("January 1170"));
  }

  @Test
  void formatISOTimeToYearSupportingBC_handlesBothInvalidMonthAndDay() {
    // Both invalid month and day "00-00" should return just the year
    String result = TimestampOperations.formatISOTimeToYearSupportingBC("1170-00-00T00:00:00Z");
    assertThat(result, equalTo("1170"));
  }

  @Test
  void formatISOTimeToYearSupportingBC_handlesInvalidBCDates() {
    // Both invalid month and day in BC dates should return just the year + B.C.
    String result = TimestampOperations.formatISOTimeToYearSupportingBC("-1170-00-00T00:00:00Z");
    assertThat(result, equalTo("1171 B.C."));
  }

  @Test
  void formatISOTimeToYearSupportingBC_handlesInvalidBCDayOnly() {
    // Invalid day in BC dates should return just the month and year + B.C.
    String result = TimestampOperations.formatISOTimeToYearSupportingBC("-1170-10-00T00:00:00Z");
    assertThat(result, equalTo("October 1171 B.C."));
  }

  @Test
  void formatISOTimeToYearSupportingBC_handlesPlussignPrefix() {
    // Handles dates with '+' prefix
    String result = TimestampOperations.formatISOTimeToYearSupportingBC("+1980-03-31T00:00:00Z");
    assertThat(result, equalTo("31 March 1980"));
  }

  @Test
  void formatISOTimeToYearSupportingBC_handlesSpecificHistoricalCase() {
    // Test for Confucius birth date
    String result = TimestampOperations.formatISOTimeToYearSupportingBC("-0552-10-09T00:00:00Z");
    assertThat(result, equalTo("09 October 0553 B.C."));
  }
}
