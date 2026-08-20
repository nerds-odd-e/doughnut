package com.odde.doughnut.services;

import com.odde.doughnut.entities.Grade;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class LearningSessionReportParser {

  public static final String SESSION_ITEM_GRADES_OPEN_TAG = "<session_item_grades>";
  public static final String SESSION_ITEM_GRADES_CLOSE_TAG = "</session_item_grades>";

  /** Legacy Report spelling; prefer {@link #SESSION_ITEM_GRADES_OPEN_TAG}. */
  public static final String SESSION_ITEM_SCORES_OPEN_TAG = "<session_item_scores>";

  /** Legacy Report spelling; prefer {@link #SESSION_ITEM_GRADES_CLOSE_TAG}. */
  public static final String SESSION_ITEM_SCORES_CLOSE_TAG = "</session_item_scores>";

  private static final Pattern GRADE_LINE = Pattern.compile("^(.+?):\\s*(\\d+)(?:\\s.*)?$");

  public record ParsedReportEntry(String noteTitle, Grade grade) {}

  public record RejectedReportEntry(String line, String reason) {}

  public record ParseResult(List<ParsedReportEntry> entries, List<RejectedReportEntry> rejected) {}

  public ParseResult parse(
      String reportMarkdown, Set<String> notebookTitles, Set<String> ambiguousTitles) {
    List<ParsedReportEntry> entries = new ArrayList<>();
    List<RejectedReportEntry> rejected = new ArrayList<>();
    Set<String> seenTitles = new HashSet<>();

    if (reportMarkdown == null || reportMarkdown.isBlank()) {
      return new ParseResult(entries, rejected);
    }

    for (String rawLine : extractGradeContent(reportMarkdown).split("\\R")) {
      String line = rawLine.trim();
      if (line.isEmpty()) {
        continue;
      }
      if (line.equalsIgnoreCase("# Learning Session Report")) {
        continue;
      }

      Matcher matcher = GRADE_LINE.matcher(line);
      if (!matcher.matches()) {
        rejected.add(new RejectedReportEntry(line, "Could not parse note title and grade."));
        continue;
      }

      String title = matcher.group(1).trim();
      int gradeValue = Integer.parseInt(matcher.group(2));
      if (gradeValue < 1 || gradeValue > 4) {
        rejected.add(new RejectedReportEntry(line, "Grade must be 1, 2, 3, or 4."));
        continue;
      }

      if (ambiguousTitles.contains(title)) {
        rejected.add(new RejectedReportEntry(line, "Ambiguous note title in notebook."));
        continue;
      }

      if (!notebookTitles.contains(title)) {
        rejected.add(new RejectedReportEntry(line, "Note title not found in notebook."));
        continue;
      }

      if (!seenTitles.add(title)) {
        rejected.add(new RejectedReportEntry(line, "Duplicate note title in report."));
        continue;
      }

      entries.add(new ParsedReportEntry(title, Grade.fromValue(gradeValue)));
    }

    return new ParseResult(entries, rejected);
  }

  private String extractGradeContent(String reportMarkdown) {
    String gradesBlock =
        extractTaggedBlock(
            reportMarkdown, SESSION_ITEM_GRADES_OPEN_TAG, SESSION_ITEM_GRADES_CLOSE_TAG);
    if (gradesBlock != null) {
      return gradesBlock;
    }
    String legacyScoresBlock =
        extractTaggedBlock(
            reportMarkdown, SESSION_ITEM_SCORES_OPEN_TAG, SESSION_ITEM_SCORES_CLOSE_TAG);
    if (legacyScoresBlock != null) {
      return legacyScoresBlock;
    }
    return reportMarkdown;
  }

  /** Returns tagged content, or null when the open tag is absent. */
  private static String extractTaggedBlock(String markdown, String openTag, String closeTag) {
    int openIndex = markdown.indexOf(openTag);
    if (openIndex < 0) {
      return null;
    }
    int contentStart = openIndex + openTag.length();
    int closeIndex = markdown.indexOf(closeTag, contentStart);
    if (closeIndex < 0) {
      return markdown.substring(contentStart);
    }
    return markdown.substring(contentStart, closeIndex);
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
