package com.odde.doughnut.services;

import com.odde.doughnut.entities.Grade;
import com.odde.doughnut.services.LearningSessionReportParser.ParseResult;
import com.odde.doughnut.services.LearningSessionReportParser.ParsedReportEntry;
import com.odde.doughnut.services.LearningSessionReportParser.RejectedReportEntry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class LearningSessionReportParseCollector {
  private final List<ParsedReportEntry> entries = new ArrayList<>();
  private final List<RejectedReportEntry> rejected = new ArrayList<>();
  private final Set<String> seenTitles = new HashSet<>();
  private final Set<String> notebookTitles;
  private final Set<String> ambiguousTitles;

  LearningSessionReportParseCollector(Set<String> notebookTitles, Set<String> ambiguousTitles) {
    this.notebookTitles = notebookTitles;
    this.ambiguousTitles = ambiguousTitles;
  }

  void reject(String displayLine, String reason) {
    rejected.add(new RejectedReportEntry(displayLine, reason));
  }

  boolean rejectIfGradeOutOfRange(String displayLine, int gradeValue) {
    if (gradeValue >= 1 && gradeValue <= 4) {
      return false;
    }
    reject(displayLine, "Grade must be 1, 2, 3, or 4.");
    return true;
  }

  void acceptEntry(String displayLine, String title, int gradeValue, String descriptiveText) {
    if (ambiguousTitles.contains(title)) {
      reject(displayLine, "Ambiguous note title in notebook.");
      return;
    }
    if (!notebookTitles.contains(title)) {
      reject(displayLine, "Note title not found in notebook.");
      return;
    }
    if (!seenTitles.add(title)) {
      reject(displayLine, "Duplicate note title in report.");
      return;
    }
    entries.add(new ParsedReportEntry(title, Grade.fromValue(gradeValue), descriptiveText));
  }

  ParseResult result() {
    return new ParseResult(entries, rejected);
  }
}
