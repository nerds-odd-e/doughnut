package com.odde.doughnut.models;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;

public class Srt {

  @Getter private String srtText;

  public Srt(String srtText) {
    this.srtText = srtText;
  }

  public String convertSrtToText() {
    StringBuilder text = new StringBuilder();
    for (String line : srtText.split("\n+")) {
      if (line.substring(0, 1).matches("[a-zA-Z]")) {
        text.append(line);
        text.append("\n");
      }
    }
    return text.toString();
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
