package com.odde.donut.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.donut.services.LearningSessionReportParser.RejectedReportEntry;

final class LearningSessionReportParseAssertions {
  private LearningSessionReportParseAssertions() {}

  static void assertRejected(RejectedReportEntry rejected, String line, String reasonFragment) {
    assertEquals(line, rejected.line());
    assertTrue(
        rejected.reason().contains(reasonFragment),
        () -> "expected reason containing '" + reasonFragment + "' but was: " + rejected.reason());
  }
}
