package com.odde.doughnut.algorithms;

import java.util.regex.Pattern;

public class ClozeDescription {
  private final String pronunciationReplacement;
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
    this.pronunciationReplacement = pronunciationReplacement;
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
    final String internalPronunciationReplacement = "__p_r_o_n_u_n_c__";
    final Pattern pattern =
        Pattern.compile("\\/[^\\s^\\/][^\\/\\n]*\\/(?!\\w)", Pattern.CASE_INSENSITIVE);
    String d = pattern.matcher(description).replaceAll(internalPronunciationReplacement);
    return new ClozedString(
        noteTitle
            .replaceTitleFragments(d, this.clozeReplacement)
            .replace(internalPronunciationReplacement, pronunciationReplacement));
  }

  public ClozedString getClozeDescriptionXX(NoteTitle noteTitle, ClozedString sourceDescription) {
    return getClozeDescription(noteTitle, sourceDescription.cloze());
  }
}
