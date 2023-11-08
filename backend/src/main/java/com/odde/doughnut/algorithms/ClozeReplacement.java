package com.odde.doughnut.algorithms;

import java.util.List;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

record ClozeReplacement(
    String partialMatchReplacement,
    String fullMatchReplacement,
    String pronunciationReplacement,
    String fullMatchSubtitleReplacement) {

  private String replaceTitleFragments(String pronunciationMasked, NoteTitle noteTitle) {
    final String internalPartialMatchReplacement = "__p_a_r_t_i_a_l__";
    final String internalFullMatchReplacement = "__f_u_l_l__";
    final String internalFullMatchReplacementForSubtitle = "__f_u_l_l_s_u_b__";

    String step1 =
        replaceTitlesWithInternalPlaceholder(
            noteTitle.getTitles(),
            pronunciationMasked,
            (p, t) -> t.replaceLiteralWords(p, internalFullMatchReplacement));
    String step2 =
        replaceTitlesWithInternalPlaceholder(
            noteTitle.getTitles(),
            step1,
            (p, t) -> t.replaceSimilar(p, internalPartialMatchReplacement));
    String step3 =
        replaceTitlesWithInternalPlaceholder(
            noteTitle.getSubtitles(),
            step2,
            (p, t) -> t.replaceLiteralWords(p, internalFullMatchReplacementForSubtitle));
    return step3
        .replace(internalFullMatchReplacement, fullMatchReplacement)
        .replace(internalPartialMatchReplacement, partialMatchReplacement)
        .replace(internalFullMatchReplacementForSubtitle, fullMatchSubtitleReplacement);
  }

  private static String replaceTitlesWithInternalPlaceholder(
      List<TitleFragment> noteTitle,
      String processed,
      BiFunction<String, TitleFragment, String> replacer) {
    return noteTitle.stream().reduce(processed, replacer, (x, y) -> y);
  }

  String maskPronunciationsAndTitles(String originalContent1, List<NoteTitle> noteTitles1) {
    final String internalPronunciationReplacement = "__p_r_o_n_u_n_c__";
    final Pattern pattern = Pattern.compile("/[^\\s^/][^/\\n]*/(?!\\w)", Pattern.CASE_INSENSITIVE);
    String pronunciationsReplaced =
        pattern.matcher(originalContent1).replaceAll(internalPronunciationReplacement);
    return noteTitles1.stream()
        .reduce(pronunciationsReplaced, this::replaceTitleFragments, (s, s2) -> s)
        .replace(internalPronunciationReplacement, pronunciationReplacement);
  }
}
