package com.odde.doughnut.algorithms;

import java.util.regex.Pattern;

class TitleFragment {
  static final String internalPartialMatchReplacement = "__p_a_r_t_i_a_l__";
  static final String internalFullMatchReplacement = "__f_u_l_l__";
  static final String internalPartialMatchReplacementForSubtitle = "__p_a_r_t_i_a_l_s_u_b__";
  static final String internalFullMatchReplacementForSubtitle = "__f_u_l_l_s_u_b__";
  private final String content;
  private final boolean subtitle;
  private final ClozePatternCreator clozePatternCreator;

  TitleFragment(String content, boolean subtitle) {
    this.subtitle = subtitle;
    String trimmed = content.trim();
    if (content.startsWith("~") || content.startsWith("〜") || content.startsWith("～")) {
      this.content = trimmed.substring(1);
      this.clozePatternCreator = new ClozePatternCreator(true);
    } else {
      this.content = trimmed;
      this.clozePatternCreator = new ClozePatternCreator(false);
    }
  }

  boolean matches(String answer) {
    return content.equalsIgnoreCase(answer.strip());
  }

  public String replaceLiteralWords(String description) {
    Pattern pattern = clozePatternCreator.getPattern(content);
    return pattern.matcher(description).replaceAll(getInternalFullMatchReplacement());
  }

  public String replaceSimilar(String literal) {
    if (content.length() < 4) {
      return literal;
    }
    String substring = content.substring(0, (content.length() + 1) * 3 / 4);
    Pattern pattern = clozePatternCreator.getPattern(substring);
    return pattern.matcher(literal).replaceAll(getInternalPartialMatchReplacement());
  }

  private String getInternalFullMatchReplacement() {
    if (subtitle) return internalFullMatchReplacementForSubtitle;
    return internalFullMatchReplacement;
  }

  private String getInternalPartialMatchReplacement() {
    if (subtitle) return internalPartialMatchReplacementForSubtitle;
    return internalPartialMatchReplacement;
  }

  public int length() {
    return content.length();
  }
}
