package com.odde.donut.services;

import com.odde.donut.services.LearningSessionReportParser.ParseResult;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Matcher;

final class SessionItemFeedbackBlockParser {
  private SessionItemFeedbackBlockParser() {}

  static ParseResult parse(String block, Set<String> notebookTitles, Set<String> ambiguousTitles) {
    LearningSessionReportParseCollector collector =
        new LearningSessionReportParseCollector(notebookTitles, ambiguousTitles);
    for (String itemBody :
        LearningSessionReportParser.extractSuccessiveTaggedBlocks(
            block,
            LearningSessionReportParser.SESSION_ITEM_OPEN_TAG,
            LearningSessionReportParser.SESSION_ITEM_CLOSE_TAG)) {
      acceptItem(itemBody, collector);
    }
    return collector.result();
  }

  private static void acceptItem(String body, LearningSessionReportParseCollector collector) {
    String[] lines = body.split("\\R", -1);
    int titleGradeLineIndex = firstNonBlankLineIndex(lines);
    String titleGradeLine = titleGradeLineIndex < 0 ? "" : lines[titleGradeLineIndex].trim();
    Matcher matcher = LearningSessionReportParser.GRADE_LINE.matcher(titleGradeLine);
    if (!matcher.matches()) {
      collector.reject(titleGradeLine, "Grade is required.");
      return;
    }
    String title = matcher.group(1).trim();
    int gradeValue = Integer.parseInt(matcher.group(2));
    if (collector.rejectIfGradeOutOfRange(titleGradeLine, gradeValue)) {
      return;
    }
    String descriptiveText =
        titleGradeLineIndex + 1 >= lines.length
            ? null
            : String.join("\n", Arrays.copyOfRange(lines, titleGradeLineIndex + 1, lines.length));
    collector.acceptEntry(titleGradeLine, title, gradeValue, descriptiveText);
  }

  private static int firstNonBlankLineIndex(String[] lines) {
    for (int i = 0; i < lines.length; i++) {
      if (!lines[i].trim().isEmpty()) {
        return i;
      }
    }
    return -1;
  }
}
