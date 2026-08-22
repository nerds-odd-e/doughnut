package com.odde.doughnut.services;

import com.odde.doughnut.services.LearningSessionReportParser.ParseResult;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SessionItemFeedbackBlockParser {
  private static final Pattern HEADING = Pattern.compile("^###\\s+(.+)$");
  private static final Pattern GRADE = Pattern.compile("^Grade:\\s*(\\d+)(?:\\s.*)?$");

  private SessionItemFeedbackBlockParser() {}

  static ParseResult parse(String block, Set<String> notebookTitles, Set<String> ambiguousTitles) {
    LearningSessionReportParseCollector collector =
        new LearningSessionReportParseCollector(notebookTitles, ambiguousTitles);
    String currentTitle = null;
    StringBuilder body = new StringBuilder();
    for (String rawLine : block.split("\\R")) {
      Matcher heading = HEADING.matcher(rawLine.trim());
      if (heading.matches()) {
        acceptItem(currentTitle, body.toString(), collector);
        currentTitle = heading.group(1).trim();
        body.setLength(0);
        continue;
      }
      if (currentTitle != null) {
        if (!body.isEmpty()) {
          body.append('\n');
        }
        body.append(rawLine);
      }
    }
    acceptItem(currentTitle, body.toString(), collector);
    return collector.result();
  }

  private static void acceptItem(
      String title, String body, LearningSessionReportParseCollector collector) {
    if (title == null || title.isEmpty()) {
      return;
    }
    String headingLine = "### " + title;
    String gradeLine = firstNonBlankLine(body);
    Matcher gradeMatcher = GRADE.matcher(gradeLine);
    if (!gradeMatcher.matches()) {
      collector.reject(headingLine, "Grade is required.");
      return;
    }
    int gradeValue = Integer.parseInt(gradeMatcher.group(1));
    if (collector.rejectIfGradeOutOfRange(gradeLine, gradeValue)) {
      return;
    }
    collector.acceptEntry(headingLine, title, gradeValue);
  }

  private static String firstNonBlankLine(String body) {
    for (String line : body.split("\\R")) {
      String trimmed = line.trim();
      if (!trimmed.isEmpty()) {
        return trimmed;
      }
    }
    return "";
  }
}
