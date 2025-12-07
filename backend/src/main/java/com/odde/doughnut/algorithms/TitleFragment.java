package com.odde.doughnut.algorithms;

import java.util.regex.Pattern;

record TitleFragment(boolean suffix, String stem) {
  public static TitleFragment from(String content) {
    // Normalize all Unicode whitespace characters to regular spaces (U+0020)
    // This includes: non-breaking space (U+00A0), CJK ideographic space (U+3000),
    // en/em spaces (U+2000-U+200B), and other Unicode whitespace characters
    String normalizedContent = normalizeWhitespace(content);
    String trimmedContent = normalizedContent.trim();
    if (trimmedContent.startsWith("~")
        || trimmedContent.startsWith("〜")
        || trimmedContent.startsWith("～")) {
      return new TitleFragment(true, trimmedContent.substring(1));
    }
    return new TitleFragment(false, trimmedContent);
  }

  private static String normalizeWhitespace(String content) {
    // Replace all Unicode whitespace characters with regular space (U+0020)
    // This handles: non-breaking space (U+00A0), CJK ideographic space (U+3000),
    // en/em spaces (U+2000-U+200B), and other Unicode whitespace characters
    // Regular spaces (U+0020) are replaced with themselves (no-op), other whitespace becomes
    // regular space
    return content.replaceAll("\\p{IsWhite_Space}", " ");
  }

  private ClozePatternCreator getClozePatternCreator() {
    return new ClozePatternCreator(suffix);
  }

  boolean matches(String answer) {
    if (answer == null) return false;
    return stem.equalsIgnoreCase(answer.strip());
  }

  public String replaceLiteralWords(String details, final String replacement) {
    Pattern pattern = getClozePatternCreator().getPattern(stem);
    return pattern.matcher(details).replaceAll(replacement);
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
