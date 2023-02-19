package com.odde.doughnut.algorithms;

import java.util.regex.Pattern;

class TitleFragment {
  static final String internalPartialMatchReplacement = "__p_a_r_t_i_a_l__";
  static final String internalFullMatchReplacement = "__f_u_l_l__";
  static final String internalFullMatchReplacementForSubtitle = "__f_u_l_l_s_u_b__";
  private final boolean subtitle;
  private final boolean suffix;
  private final String stem;

  TitleFragment(String content, boolean subtitle) {
    this.subtitle = subtitle;
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

  public String replaceLiteralWords(String description) {
    Pattern pattern = getClozePatternCreator().getPattern(stem);
    return pattern.matcher(description).replaceAll(getInternalFullMatchReplacement());
  }

  public String replaceSimilar(String literal) {
    String substring = stem.substring(0, (stem.length() + 1) * 3 / 4);
    Pattern pattern = getClozePatternCreator().getPattern(substring);
    return pattern.matcher(literal).replaceAll(internalPartialMatchReplacement);
  }

  private String getInternalFullMatchReplacement() {
    if (subtitle) return internalFullMatchReplacementForSubtitle;
    return internalFullMatchReplacement;
  }

  public int length() {
    return stem.length();
  }
}
