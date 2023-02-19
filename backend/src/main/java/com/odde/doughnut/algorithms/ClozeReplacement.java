package com.odde.doughnut.algorithms;

class ClozeReplacement {
  private static final String internalPartialMatchReplacement = "__p_a_r_t_i_a_l__";
  private static final String internalFullMatchReplacement = "__f_u_l_l__";
  private static final String internalFullMatchReplacementForSubtitle = "__f_u_l_l_s_u_b__";

  private final String fullMatchReplacement;
  private final String partialMatchReplacement;
  private final String fullMatchSubtitleReplacement;

  public String pronunciationReplacement;

  public ClozeReplacement(
      String partialMatchReplacement,
      String fullMatchReplacement,
      String pronunciationReplacement,
      String fullMatchSubtitleReplacement) {
    this.partialMatchReplacement = partialMatchReplacement;
    this.fullMatchReplacement = fullMatchReplacement;
    this.fullMatchSubtitleReplacement = fullMatchSubtitleReplacement;
    this.pronunciationReplacement = pronunciationReplacement;
  }

  private String replaceMasks(String titleMasked) {
    return titleMasked
        .replace(internalFullMatchReplacement, fullMatchReplacement)
        .replace(internalPartialMatchReplacement, partialMatchReplacement)
        .replace(internalFullMatchReplacementForSubtitle, fullMatchSubtitleReplacement);
  }

  String replaceTitleFragments(String pronunciationMasked, NoteTitle noteTitle) {
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
}
