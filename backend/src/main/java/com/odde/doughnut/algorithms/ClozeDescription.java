package com.odde.doughnut.algorithms;

public class ClozeDescription {
  private final ClozeReplacement clozeReplacement = new ClozeReplacement();

  public ClozeDescription(
      String partialMatchReplacement,
      String fullMatchReplacement,
      String pronunciationReplacement,
      String partialMatchSubtitleReplacement,
      String fullMatchSubtitleReplacement) {
    clozeReplacement.partialMatchReplacement = partialMatchReplacement;
    clozeReplacement.fullMatchReplacement = fullMatchReplacement;
    clozeReplacement.partialMatchSubtitleReplacement = partialMatchSubtitleReplacement;
    clozeReplacement.fullMatchSubtitleReplacement = fullMatchSubtitleReplacement;
    clozeReplacement.pronunciationReplacement = pronunciationReplacement;
  }

  public static ClozeDescription htmlClosedDescription() {
    return new ClozeDescription(
        "<mark title='Hidden text that is partially matching the answer'>[..~]</mark>",
        "<mark title='Hidden text that is matching the answer'>[...]</mark>",
        "<mark title='Hidden pronunciation'>/.../</mark>",
        "<mark title='Hidden subtitle that is partially matching the answer'>(..~)</mark>",
        "<mark title='Hidden subtitle that is matching the answer'>(...)</mark>");
  }

  public ClozedString getClozeDescription(NoteTitle noteTitle, String description) {
    return new ClozedString(this.clozeReplacement, description).hide(noteTitle);
  }

  public ClozedString getClozeDescriptionXX(NoteTitle noteTitle, ClozedString sourceDescription) {
    return sourceDescription.hide(noteTitle);
  }
}
