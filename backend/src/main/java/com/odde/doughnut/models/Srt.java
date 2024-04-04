package com.odde.doughnut.models;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Srt {
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss,SSS");
  public static final int GAP_TIME = 2 * 60;

  private final String srtText;

  private static final String ARROW_REGEX = " --> ";

  private static final String NEWLINE_REGEX = "\n";

  private static final String SPACE_REGEX = "\s";

  private static final String TIME_CODE_PATTERN =
      "\\d{2}:\\d{2}:\\d{2},\\d{3} --> \\d{2}:\\d{2}:\\d{2},\\d{3}";

  private static final String SUBTITLE_PATTERN = "[^\\s].*";

  public Srt(String srtText) {
    this.srtText = srtText;
  }

  public String convertSrtToText() {
    StringBuilder text = new StringBuilder();
    // Split the SRT first
    String[] paragraphs = srtText.split("\n\n");
    for (int i = 0; i < paragraphs.length; i++) {

      String[] lines = paragraphs[i].split(NEWLINE_REGEX);

      LocalTime endTime = parseTimestamp(lines[1].split(ARROW_REGEX)[1]);
      text.append(lines[2]);

      checkForParagraph(i, paragraphs, endTime, text);
    }
    return text.toString();
  }

  private void checkForParagraph(
      int i, String[] paragraphs, LocalTime endTime, StringBuilder text) {
    if (i < paragraphs.length - 1) {
      String nextStartTimeStr = paragraphs[i + 1].split(NEWLINE_REGEX)[1].split(ARROW_REGEX)[0];
      LocalTime nextStartTime = parseTimestamp(nextStartTimeStr);
      Duration gap = Duration.between(endTime, nextStartTime);
      if (gap.getSeconds() >= GAP_TIME) {
        text.append(NEWLINE_REGEX);
      } else {
        text.append(SPACE_REGEX);
      }
    }
  }

  private LocalTime parseTimestamp(String timestamp) {
    return LocalTime.parse(timestamp, FORMATTER);
  }

  public boolean isSRTFormat() {
    if (srtText == null || srtText.isBlank()) {
      return false;
    }
    Pattern pattern = Pattern.compile(TIME_CODE_PATTERN + "\\n" + SUBTITLE_PATTERN);
    Matcher matcher = pattern.matcher(srtText);
    return matcher.find();
  }
}
