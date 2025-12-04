package com.odde.doughnut.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class TimezoneUtilsTest {
  @Test
  void shouldParseValidTimezone() {
    ZoneId result = TimezoneUtils.parseTimezone("Asia/Shanghai");
    assertThat(result, equalTo(ZoneId.of("Asia/Shanghai")));
  }

  @Test
  void shouldDefaultToUTCForInvalidTimezone() {
    ZoneId result = TimezoneUtils.parseTimezone("Etc/Unknown");
    assertThat(result, equalTo(ZoneId.of("UTC")));
  }

  @Test
  void shouldDefaultToUTCForEmptyString() {
    ZoneId result = TimezoneUtils.parseTimezone("");
    assertThat(result, equalTo(ZoneId.of("UTC")));
  }

  @Test
  void shouldDefaultToUTCForNull() {
    ZoneId result = TimezoneUtils.parseTimezone(null);
    assertThat(result, equalTo(ZoneId.of("UTC")));
  }
}
