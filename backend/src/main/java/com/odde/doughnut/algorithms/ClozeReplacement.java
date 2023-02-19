package com.odde.doughnut.algorithms;

import java.util.List;
import java.util.regex.Pattern;

record ClozeReplacement(
    String partialMatchReplacement,
    String fullMatchReplacement,
    String pronunciationReplacement,
    String fullMatchSubtitleReplacement) {
  private static final String internalPartialMatchReplacement = "__p_a_r_t_i_a_l__";
  private static final String internalFullMatchReplacement = "__f_u_l_l__";
  private static final String internalFullMatchReplacementForSubtitle = "__f_u_l_l_s_u_b__";

  private String replaceMasks(String titleMasked) {
    return titleMasked
        .replace(internalFullMatchReplacement, fullMatchReplacement)
        .replace(internalPartialMatchReplacement, partialMatchReplacement)
        .replace(internalFullMatchReplacementForSubtitle, fullMatchSubtitleReplacement);
  }

  private String replaceTitleFragments(String pronunciationMasked, NoteTitle noteTitle) {
    String literalMatchPreMasked =
        noteTitle.getTitles().stream()
            .reduce(
                pronunciationMasked,
                (d, t) -> t.replaceLiteralWords(d, internalFullMatchReplacement),
                (x, y) -> y);
    String titlePreMasked =
        noteTitle.getTitles().stream()
            .reduce(
                literalMatchPreMasked,
                (d, t) -> t.replaceSimilar(d, internalPartialMatchReplacement),
                (x, y) -> y);
    String titleAllPreMasked =
        noteTitle.getSubtitles().stream()
            .reduce(
                titlePreMasked,
                (d, t) -> t.replaceLiteralWords(d, internalFullMatchReplacementForSubtitle),
                (x, y) -> y);
    return replaceMasks(titleAllPreMasked);
  }

  String maskPronunciationsAndTitles(String originalContent1, List<NoteTitle> noteTitles1) {
    final String internalPronunciationReplacement = "__p_r_o_n_u_n_c__";
    final Pattern pattern =
        Pattern.compile("\\/[^\\s^\\/][^\\/\\n]*\\/(?!\\w)", Pattern.CASE_INSENSITIVE);
    String pronunciationsReplaced =
        pattern.matcher(originalContent1).replaceAll(internalPronunciationReplacement);
    return noteTitles1.stream()
        .reduce(pronunciationsReplaced, this::replaceTitleFragments, (s, s2) -> s)
        .replace(internalPronunciationReplacement, pronunciationReplacement);
  }
}
