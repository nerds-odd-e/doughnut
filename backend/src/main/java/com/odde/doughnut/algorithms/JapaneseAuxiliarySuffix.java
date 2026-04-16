package com.odde.doughnut.algorithms;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Longest-match auxiliary endings (hiragana/katakana) immediately after a cloze stem match so the
 * mask can leave trailing morphemes visible (e.g. after matching {@code 食べ} in {@code 食べた},
 * keep {@code た} outside the mask).
 */
final class JapaneseAuxiliarySuffix {

  private static final String[] SUFFIXES = {
    "ませんでした",
    "ではありませんでした",
    "じゃありませんでした",
    "ではなかった",
    "じゃなかった",
    "でありませんでした",
    "でいませんでした",
    "でありません",
    "でいません",
    "でありました",
    "でいました",
    "であります",
    "でいます",
    "でした",
    "だった",
    "でしょう",
    "だろう",
    "ません",
    "ました",
    "ましょうか",
    "ましょう",
    "でしょうか",
    "ではない",
    "じゃない",
    "ありません",
    "なかった",
    "なければ",
    "ちゃった",
    "ちゃう",
    "じゃう",
    "すぎる",
    "ください",
    "ければ",
    "けれど",
    "がる",
    "そう",
    "かった",
    "くない",
    "であり",
    "でいる",
    "である",
    "であった",
    "です",
    "ます",
    "ない",
    "った",
    "って",
    "んで",
    "して",
    "いで",
    "た",
    "て",
    "だ",
    "で",
    "じゃ",
  };

  private static final List<String> BY_LENGTH_DESC =
      Arrays.stream(SUFFIXES)
          .sorted(Comparator.comparingInt(String::length).reversed())
          .distinct()
          .toList();

  private JapaneseAuxiliarySuffix() {}

  /**
   * Longest known auxiliary that is a prefix of {@code remainder} (must start with kana), or
   * empty.
   */
  static String longestPrefixRemainder(String remainder) {
    if (remainder == null || remainder.isEmpty()) {
      return "";
    }
    int first = remainder.codePointAt(0);
    if (!isKanaCodePoint(first)) {
      return "";
    }
    for (String suffix : BY_LENGTH_DESC) {
      if (remainder.startsWith(suffix)
          && suffix.chars().allMatch(JapaneseAuxiliarySuffix::isKanaCodePoint)) {
        return suffix;
      }
    }
    return "";
  }

  private static boolean isKanaCodePoint(int cp) {
    Character.UnicodeScript script = Character.UnicodeScript.of(cp);
    return script == Character.UnicodeScript.HIRAGANA
        || script == Character.UnicodeScript.KATAKANA;
  }
}
