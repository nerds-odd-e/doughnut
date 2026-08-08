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

  private static final Pattern SCORE_LINE = Pattern.compile("^(.+?):\\s*(\\d+)(?:\\s.*)?$");

  public record ParsedReportEntry(String noteTitle, int score) {}

  public record RejectedReportEntry(String line, String reason) {}

  public record ParseResult(List<ParsedReportEntry> entries, List<RejectedReportEntry> rejected) {}

  public ParseResult parse(String reportMarkdown) {
    return parse(reportMarkdown, Set.of(), Set.of());
  }

  public ParseResult parse(
      String reportMarkdown, Set<String> sessionItemTitles, Set<String> ambiguousTitles) {
    List<ParsedReportEntry> entries = new ArrayList<>();
    List<RejectedReportEntry> rejected = new ArrayList<>();

    if (reportMarkdown == null || reportMarkdown.isBlank()) {
      return new ParseResult(entries, rejected);
    }

    for (String rawLine : reportMarkdown.split("\\R")) {
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

      if (!sessionItemTitles.isEmpty() && !sessionItemTitles.contains(title)) {
        rejected.add(new RejectedReportEntry(line, "No session item matched this note title."));
        continue;
      }

      entries.add(new ParsedReportEntry(title, score));
    }

    return new ParseResult(entries, rejected);
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
