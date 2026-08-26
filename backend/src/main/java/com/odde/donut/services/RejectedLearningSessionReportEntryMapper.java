package com.odde.donut.services;

import com.odde.donut.controllers.dto.RejectedLearningSessionReportEntry;
import com.odde.donut.services.LearningSessionReportParser.RejectedReportEntry;
import java.util.List;

public final class RejectedLearningSessionReportEntryMapper {

  private RejectedLearningSessionReportEntryMapper() {}

  public static List<RejectedLearningSessionReportEntry> fromParsed(
      List<RejectedReportEntry> rejected) {
    return rejected.stream().map(entry -> of(entry.line(), entry.reason())).toList();
  }

  public static RejectedLearningSessionReportEntry of(String line, String reason) {
    RejectedLearningSessionReportEntry dto = new RejectedLearningSessionReportEntry();
    dto.setLine(line);
    dto.setReason(reason);
    return dto;
  }
}
