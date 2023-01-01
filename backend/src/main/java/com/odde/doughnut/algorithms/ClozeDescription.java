package com.odde.doughnut.algorithms;

import static com.odde.doughnut.algorithms.ClozedString.internalPronunciationReplacement;

import java.util.regex.Pattern;

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
    final Pattern pattern =
        Pattern.compile("\\/[^\\s^\\/][^\\/\\n]*\\/(?!\\w)", Pattern.CASE_INSENSITIVE);
    String d = pattern.matcher(description).replaceAll(internalPronunciationReplacement);
    return new ClozedString(this.clozeReplacement, d, noteTitle);
  }

  public ClozedString getClozeDescriptionXX(NoteTitle noteTitle, ClozedString sourceDescription) {
    return getClozeDescription(noteTitle, sourceDescription.cloze());
  }
}
