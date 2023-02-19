package com.odde.doughnut.algorithms;

import java.util.regex.Pattern;

class TitleFragment {
  private final boolean suffix;
  private final String stem;

  TitleFragment(String content) {
    String trimmedContent = content.trim();
    this.suffix = content.startsWith("~") || content.startsWith("〜") || content.startsWith("～");
    if (suffix) {
      this.stem = trimmedContent.substring(1);
    } else {
      this.stem = trimmedContent;
    }
  }

  private ClozePatternCreator getClozePatternCreator() {
    return new ClozePatternCreator(suffix);
  }

  boolean matches(String answer) {
    return stem.equalsIgnoreCase(answer.strip());
  }

  public String replaceLiteralWords(String description, final String replacement) {
    Pattern pattern = getClozePatternCreator().getPattern(stem);
    return pattern.matcher(description).replaceAll(replacement);
  }

  public String replaceSimilar(String literal, String replacement) {
    String substring = stem.substring(0, (stem.length() + 1) * 3 / 4);
    Pattern pattern = getClozePatternCreator().getPattern(substring);
    return pattern.matcher(literal).replaceAll(replacement);
  }

  public int length() {
    return stem.length();
  }
}
