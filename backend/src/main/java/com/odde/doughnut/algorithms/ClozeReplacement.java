package com.odde.doughnut.algorithms;

class ClozeReplacement {

  public String fullMatchReplacement;
  public String partialMatchReplacement;
  public String fullMatchSubtitleReplacement;
  public String partialMatchSubtitleReplacement;

  String replaceMasks(String titleMasked) {
    return titleMasked
        .replace(TitleFragment.internalFullMatchReplacement, fullMatchReplacement)
        .replace(TitleFragment.internalPartialMatchReplacement, partialMatchReplacement)
        .replace(
            TitleFragment.internalFullMatchReplacementForSubtitle, fullMatchSubtitleReplacement)
        .replace(
            TitleFragment.internalPartialMatchReplacementForSubtitle,
            partialMatchSubtitleReplacement);
  }
}
