package com.odde.doughnut.models;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Srt {
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss,SSS");

  private String srtText;

  public Srt(String srtText) {
    this.srtText = srtText;
  }

  public String convertSrtToText() {
    StringBuilder text = new StringBuilder();
    String[] paragraphs = srtText.split("\n\n");
    for (int i = 0; i < paragraphs.length; i++) {
      String[] lines = paragraphs[i].split("\n");
      LocalTime endTime = parseTimestamp(lines[1].split(" --> ")[1]);
      text.append(lines[2]);
      for (int j = 3; j < lines.length; j++) {
        text.append(" ").append(lines[j]);
      }

      if (i < paragraphs.length - 1) {
        String nextStartTimeStr = paragraphs[i + 1].split("\n")[1].split(" --> ")[0];
        LocalTime nextStartTime = parseTimestamp(nextStartTimeStr);
        Duration gap = Duration.between(endTime, nextStartTime);
        if (gap.getSeconds() >= (2 * 60)) {
          text.append("\n");
        } else {
          text.append("\s");
        }
      }
    }
    return text.toString();
  }

  private LocalTime parseTimestamp(String timestamp) {
    return LocalTime.parse(timestamp, FORMATTER);
  }

  public boolean isSRTFormat() {
    if (srtText == null || srtText.isBlank()) {
      return false;
    }
    String timeCodePattern = "\\d{2}:\\d{2}:\\d{2},\\d{3} --> \\d{2}:\\d{2}:\\d{2},\\d{3}";
    String subtitlePattern = "[^\\s].*";
    Pattern pattern = Pattern.compile(timeCodePattern + "\\n" + subtitlePattern);
    Matcher matcher = pattern.matcher(srtText);
    return matcher.find();
  }
}
