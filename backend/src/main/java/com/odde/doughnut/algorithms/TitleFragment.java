package com.odde.doughnut.algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/** A parsed recall or qualifier fragment: {@code stem} plus an optional cloze suffix marker. */
record TitleFragment(boolean suffixMarker, String stem) {
  public static TitleFragment from(String content) {
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
    return new ClozePatternCreator(suffixMarker);
  }

  boolean matches(String answer) {
    if (answer == null) return false;
    return stem.equalsIgnoreCase(answer.strip());
  }

  public String replaceLiteralWords(String text, final String replacement) {
    Pattern pattern = getClozePatternCreator().getPattern(stem);
    return pattern.matcher(text).replaceAll(replacement);
  }

  public String replaceSimilar(String literal, String replacement) {
    if (JapaneseLemmaStemMasker.isEligibleLemma(stem)) {
      String masked = JapaneseLemmaStemMasker.maskConjugations(literal, stem, replacement);
      if (!masked.equals(literal)) {
        return masked;
      }
    }
    String substring = stem.substring(0, (stem.length() + 1) * 3 / 4);
    Pattern pattern = getClozePatternCreator().getPattern(substring);
    return pattern.matcher(literal).replaceAll(replacement);
  }

  public int length() {
    return stem.length();
  }

  static List<TitleFragment> sortedLongestFirst(List<TitleFragment> fragments) {
    if (fragments.size() <= 1) {
      return fragments;
    }
    List<TitleFragment> sorted = new ArrayList<>(fragments);
    sorted.sort(Comparator.comparing(TitleFragment::length));
    Collections.reverse(sorted);
    return sorted;
  }

  static List<TitleFragment> mergeSortedLongestFirst(
      List<TitleFragment> titleFragments, List<TitleFragment> additionalFragments) {
    if (additionalFragments.isEmpty()) {
      return titleFragments;
    }
    List<TitleFragment> merged = new ArrayList<>(titleFragments);
    merged.addAll(additionalFragments);
    return sortedLongestFirst(merged);
  }
}
