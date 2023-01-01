package com.odde.doughnut.algorithms;

import org.apache.logging.log4j.util.Strings;

public class ClozedString {
  private ClozeReplacement clozeReplacement;
  private String cnt;
  private NoteTitle noteTitle;

  static final String internalPronunciationReplacement = "__p_r_o_n_u_n_c__";

  public ClozedString(ClozeReplacement clozeReplacement, String cnt, NoteTitle noteTitle) {
    this.clozeReplacement = clozeReplacement;

    this.cnt = cnt;
    this.noteTitle = noteTitle;
  }

  @Override
  public String toString() {
    throw new RuntimeException("Not implemented, use `cloze` instead.");
  }

  public String cloze() {
    return clozeReplacement
        .replaceTitleFragments(cnt, noteTitle)
        .replace(internalPronunciationReplacement, clozeReplacement.pronunciationReplacement);
  }

  public boolean isPresent() {
    return Strings.isNotBlank(cnt);
  }
}
