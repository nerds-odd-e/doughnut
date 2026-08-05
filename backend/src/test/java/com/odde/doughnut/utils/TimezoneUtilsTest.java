package com.odde.doughnut.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class TimezoneUtilsTest {
  @Test
  void shouldParseValidTimezone() {
    assertThat(TimezoneUtils.parseTimezone("Asia/Shanghai"), equalTo(ZoneId.of("Asia/Shanghai")));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"Etc/Unknown"})
  void shouldDefaultToUTCForMissingOrInvalidTimezone(String timezone) {
    assertThat(TimezoneUtils.parseTimezone(timezone), equalTo(ZoneId.of("UTC")));
  }
}
