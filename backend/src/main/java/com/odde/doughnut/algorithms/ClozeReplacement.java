package com.odde.doughnut.algorithms;

class ClozeReplacement {

  public String fullMatchReplacement;
  public String partialMatchReplacement;
  public String fullMatchSubtitleReplacement;
  public String partialMatchSubtitleReplacement;

  public String pronunciationReplacement;

  private String replaceMasks(String titleMasked) {
    return titleMasked
        .replace(TitleFragment.internalFullMatchReplacement, fullMatchReplacement)
        .replace(TitleFragment.internalPartialMatchReplacement, partialMatchReplacement)
        .replace(
            TitleFragment.internalFullMatchReplacementForSubtitle, fullMatchSubtitleReplacement)
        .replace(
            TitleFragment.internalPartialMatchReplacementForSubtitle,
            partialMatchSubtitleReplacement);
  }

  String replaceTitleFragments(String pronunciationMasked, NoteTitle noteTitle) {
    String literalMatchPreMasked =
        noteTitle.getTitles().stream()
            .reduce(pronunciationMasked, (d, t) -> t.replaceLiteralWords(d), (x, y) -> y);
    String titlePreMasked =
        noteTitle.getTitles().stream()
            .reduce(literalMatchPreMasked, (d, t) -> t.replaceSimilar(d), (x, y) -> y);
    return replaceMasks(titlePreMasked);
  }
}
