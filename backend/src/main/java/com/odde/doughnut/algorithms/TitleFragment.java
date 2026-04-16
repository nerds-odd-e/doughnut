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
    return replaceEachMatchKeepingJapaneseAuxiliaryTail(
        getClozePatternCreator().getPattern(stem),
        details,
        replacement,
        replacement,
        stem,
        suffix,
        false);
  }

  /**
   * Like {@link #replaceLiteralWords} but when a suffix-title stem keeps a following auxiliary
   * visible, use {@code partialWhenAuxiliaryTail} so the recall stem shows a partial cloze
   * ([..~]) rather than a full one ([...]).
   */
  public String replaceLiteralWords(
      String details, String fullReplacement, String partialWhenAuxiliaryTail) {
    return replaceEachMatchKeepingJapaneseAuxiliaryTail(
        getClozePatternCreator().getPattern(stem),
        details,
        fullReplacement,
        partialWhenAuxiliaryTail,
        stem,
        suffix,
        false);
  }

  public String replaceSimilar(String literal, String replacement) {
    String substring = stem.substring(0, (stem.length() + 1) * 3 / 4);
    return replaceEachMatchKeepingJapaneseAuxiliaryTail(
        getClozePatternCreator().getPattern(substring),
        literal,
        replacement,
        replacement,
        stem,
        suffix,
        true);
  }

  /**
   * When the match is a strict prefix of the note title stem (partial match pass), or the title
   * uses suffix mode ({@code ~stem}) on the literal pass, keep a following auxiliary visible (e.g.
   * {@code 食べ} + {@code た}).
   */
  private static String replaceEachMatchKeepingJapaneseAuxiliaryTail(
      Pattern pattern,
      String details,
      String defaultMaskReplacement,
      String partialWhenAuxiliaryTail,
      String fullStem,
      boolean suffixTitle,
      boolean fromSimilarPass) {
    Matcher matcher = pattern.matcher(details);
    StringBuilder out = new StringBuilder();
    int lastAppend = 0;
    while (matcher.find()) {
      String matched = matcher.group();
      int matchEnd = matcher.end();
      String remainder = details.substring(matchEnd);
      String auxiliaryTail = JapaneseAuxiliarySuffix.longestPrefixRemainder(remainder);
      boolean allowAuxiliaryTail =
          fromSimilarPass
              ? matched.length() < fullStem.length()
              : suffixTitle && !auxiliaryTail.isEmpty();
      String tail = allowAuxiliaryTail ? auxiliaryTail : "";
      String maskReplacement =
          !tail.isEmpty() && suffixTitle && !fromSimilarPass
              ? partialWhenAuxiliaryTail
              : defaultMaskReplacement;
      out.append(details, lastAppend, matcher.start());
      out.append(maskReplacement).append(tail);
      lastAppend = matchEnd + tail.length();
    }
    out.append(details, lastAppend, details.length());
    return out.toString();
  }

  public int length() {
    return stem.length();
  }
}
