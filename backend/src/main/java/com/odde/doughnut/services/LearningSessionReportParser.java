package com.odde.doughnut.services;

import java.util.ArrayList;
import java.util.List;
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

      entries.add(new ParsedReportEntry(title, score));
    }

    return new ParseResult(entries, rejected);
  }
}
