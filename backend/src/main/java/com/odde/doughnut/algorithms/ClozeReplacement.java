package com.odde.doughnut.algorithms;

class ClozeReplacement {

  public String fullMatchReplacement;
  public String partialMatchReplacement;
  public String fullMatchSubtitleReplacement;
  public String partialMatchSubtitleReplacement;

  public String pronunciationReplacement;

  public ClozeReplacement(
      String partialMatchReplacement,
      String fullMatchReplacement,
      String pronunciationReplacement,
      String partialMatchSubtitleReplacement,
      String fullMatchSubtitleReplacement) {
    this.partialMatchReplacement = partialMatchReplacement;
    this.fullMatchReplacement = fullMatchReplacement;
    this.partialMatchSubtitleReplacement = partialMatchSubtitleReplacement;
    this.fullMatchSubtitleReplacement = fullMatchSubtitleReplacement;
    this.pronunciationReplacement = pronunciationReplacement;
  }

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
    String titleAllPreMasked =
        noteTitle.getSubtitles().stream()
            .reduce(titlePreMasked, (d, t) -> t.replaceLiteralWords(d), (x, y) -> y);
    return replaceMasks(titleAllPreMasked);
  }
}
