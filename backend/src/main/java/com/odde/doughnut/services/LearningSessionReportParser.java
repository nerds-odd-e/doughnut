package com.odde.doughnut.services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class LearningSessionReportParser {

  public static final String SESSION_ITEM_SCORES_OPEN_TAG = "<session_item_scores>";
  public static final String SESSION_ITEM_SCORES_CLOSE_TAG = "</session_item_scores>";

  private static final Pattern SCORE_LINE = Pattern.compile("^(.+?):\\s*(\\d+)(?:\\s.*)?$");

  public record ParsedReportEntry(String noteTitle, int score) {}

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

    for (String rawLine : extractScoreContent(reportMarkdown).split("\\R")) {
      String line = rawLine.trim();
      if (line.isEmpty()) {
        continue;
      }
      if (line.equalsIgnoreCase("# Learning Session Report")) {
        continue;
      }

      Matcher matcher = SCORE_LINE.matcher(line);
      if (!matcher.matches()) {
        rejected.add(new RejectedReportEntry(line, "Could not parse note title and score."));
        continue;
      }

      String title = matcher.group(1).trim();
      int score = Integer.parseInt(matcher.group(2));
      if (score < 0 || score > 5) {
        rejected.add(new RejectedReportEntry(line, "Score must be between 0 and 5."));
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

      entries.add(new ParsedReportEntry(title, score));
    }

    return new ParseResult(entries, rejected);
  }

  private String extractScoreContent(String reportMarkdown) {
    int openIndex = reportMarkdown.indexOf(SESSION_ITEM_SCORES_OPEN_TAG);
    if (openIndex < 0) {
      return reportMarkdown;
    }

    int contentStart = openIndex + SESSION_ITEM_SCORES_OPEN_TAG.length();
    int closeIndex = reportMarkdown.indexOf(SESSION_ITEM_SCORES_CLOSE_TAG, contentStart);
    if (closeIndex < 0) {
      return reportMarkdown.substring(contentStart);
    }
    return reportMarkdown.substring(contentStart, closeIndex);
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
