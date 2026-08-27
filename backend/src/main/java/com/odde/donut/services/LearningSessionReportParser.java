package com.odde.donut.services;

import com.odde.donut.entities.Grade;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class LearningSessionReportParser {

  public static final String SESSION_ITEM_FEEDBACK_OPEN_TAG = "<session_item_feedback>";
  public static final String SESSION_ITEM_FEEDBACK_CLOSE_TAG = "</session_item_feedback>";

  public static final String SESSION_ITEM_OPEN_TAG = "<session_item>";
  public static final String SESSION_ITEM_CLOSE_TAG = "</session_item>";

  public static final String SESSION_ITEM_GRADES_OPEN_TAG = "<session_item_grades>";
  public static final String SESSION_ITEM_GRADES_CLOSE_TAG = "</session_item_grades>";

  /** Legacy Report spelling; prefer {@link #SESSION_ITEM_GRADES_OPEN_TAG}. */
  public static final String SESSION_ITEM_SCORES_OPEN_TAG = "<session_item_scores>";

  /** Legacy Report spelling; prefer {@link #SESSION_ITEM_GRADES_CLOSE_TAG}. */
  public static final String SESSION_ITEM_SCORES_CLOSE_TAG = "</session_item_scores>";

  static final Pattern GRADE_LINE = Pattern.compile("^(.+?):\\s*(\\d+)(?:\\s.*)?$");

  public record ParsedReportEntry(String noteTitle, Grade grade, String descriptiveText) {
    public ParsedReportEntry {
      descriptiveText =
          descriptiveText == null || descriptiveText.isBlank() ? null : descriptiveText.strip();
    }
  }

  public record RejectedReportEntry(String line, String reason) {}

  public record ParseResult(List<ParsedReportEntry> entries, List<RejectedReportEntry> rejected) {}

  public ParseResult parse(
      String reportMarkdown, Set<String> notebookTitles, Set<String> ambiguousTitles) {
    if (reportMarkdown == null || reportMarkdown.isBlank()) {
      return new ParseResult(List.of(), List.of());
    }

    String feedbackBlock =
        extractTaggedBlock(
            reportMarkdown, SESSION_ITEM_FEEDBACK_OPEN_TAG, SESSION_ITEM_FEEDBACK_CLOSE_TAG);
    if (feedbackBlock != null) {
      return SessionItemFeedbackBlockParser.parse(feedbackBlock, notebookTitles, ambiguousTitles);
    }
    return parseGradeLines(extractGradeContent(reportMarkdown), notebookTitles, ambiguousTitles);
  }

  private ParseResult parseGradeLines(
      String gradeContent, Set<String> notebookTitles, Set<String> ambiguousTitles) {
    LearningSessionReportParseCollector collector =
        new LearningSessionReportParseCollector(notebookTitles, ambiguousTitles);

    for (String rawLine : gradeContent.split("\\R")) {
      String line = rawLine.trim();
      if (line.isEmpty()) {
        continue;
      }
      if (line.equalsIgnoreCase("# Learning Session Report")) {
        continue;
      }

      Matcher matcher = GRADE_LINE.matcher(line);
      if (!matcher.matches()) {
        collector.reject(line, "Could not parse note title and grade.");
        continue;
      }

      String title = matcher.group(1).trim();
      int gradeValue = Integer.parseInt(matcher.group(2));
      if (collector.rejectIfGradeOutOfRange(line, gradeValue)) {
        continue;
      }
      collector.acceptEntry(line, title, gradeValue, null);
    }

    return collector.result();
  }

  private String extractGradeContent(String reportMarkdown) {
    String gradesBlock =
        extractTaggedBlock(
            reportMarkdown, SESSION_ITEM_GRADES_OPEN_TAG, SESSION_ITEM_GRADES_CLOSE_TAG);
    if (gradesBlock != null && !gradesBlock.isBlank()) {
      return gradesBlock;
    }
    String legacyScoresBlock =
        extractTaggedBlock(
            reportMarkdown, SESSION_ITEM_SCORES_OPEN_TAG, SESSION_ITEM_SCORES_CLOSE_TAG);
    if (legacyScoresBlock != null) {
      return legacyScoresBlock;
    }
    return gradesBlock != null ? gradesBlock : reportMarkdown;
  }

  /** Returns tagged content, or null when the open tag is absent. */
  private static String extractTaggedBlock(String markdown, String openTag, String closeTag) {
    List<String> blocks = extractSuccessiveTaggedBlocks(markdown, openTag, closeTag);
    return blocks.isEmpty() ? null : blocks.getFirst();
  }

  /** Each open tag starts a block; a missing close tag runs to the end of markdown. */
  static List<String> extractSuccessiveTaggedBlocks(
      String markdown, String openTag, String closeTag) {
    List<String> blocks = new ArrayList<>();
    int searchFrom = 0;
    while (true) {
      int openIndex = markdown.indexOf(openTag, searchFrom);
      if (openIndex < 0) {
        return blocks;
      }
      int contentStart = openIndex + openTag.length();
      int closeIndex = markdown.indexOf(closeTag, contentStart);
      if (closeIndex < 0) {
        blocks.add(markdown.substring(contentStart));
        return blocks;
      }
      blocks.add(markdown.substring(contentStart, closeIndex));
      searchFrom = closeIndex + closeTag.length();
    }
  }

  public static Set<String> ambiguousTitles(Iterable<String> titles) {
    Set<String> seen = new HashSet<>();
    Set<String> ambiguous = new HashSet<>();
    for (String title : titles) {
      if (!seen.add(title)) {
        ambiguous.add(title);
      }
    }
    return ambiguous;
  }
}
