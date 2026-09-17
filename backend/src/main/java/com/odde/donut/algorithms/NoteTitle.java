package com.odde.donut.algorithms;

import java.util.List;

/**
 * The whole title text is one literal recall fragment, except that a {@code ~}/{@code 〜}/{@code ～}
 * marker at its actual beginning still marks a suffix fragment.
 */
public class NoteTitle {

  private final String rawTitle;

  public NoteTitle(String rawTitle) {
    this.rawTitle = rawTitle;
  }

  public boolean matchesForRecall(String answer) {
    return titleFragment().matches(answer);
  }

  /** Title fragments for cloze masking. */
  public List<TitleFragment> getRecallTitleFragments() {
    return List.of(titleFragment());
  }

  private TitleFragment titleFragment() {
    return TitleFragment.from(rawTitle);
  }
}
