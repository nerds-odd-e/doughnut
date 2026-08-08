package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.RejectedLearningSessionReportEntry;
import com.odde.doughnut.services.LearningSessionReportParser.RejectedReportEntry;
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
