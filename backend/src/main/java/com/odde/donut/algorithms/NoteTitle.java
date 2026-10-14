package com.odde.donut.algorithms;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The whole title text is one literal cloze fragment. Recall matching additionally accepts the
 * title without its final round-parenthesized content. A {@code ~}/{@code 〜}/{@code ～} marker at
 * the actual beginning still marks a suffix fragment.
 */
public class NoteTitle {

  private static final Pattern TITLE_WITH_TRAILING_PARENTHESIZED_CONTENT =
      Pattern.compile("(?s)(.+)\\([^()]+\\)\\s*$");

  private final String rawTitle;

  public NoteTitle(String rawTitle) {
    this.rawTitle = rawTitle;
  }

  public boolean matchesForRecall(String answer) {
    if (titleFragment().matches(answer)) {
      return true;
    }
    Matcher matcher = TITLE_WITH_TRAILING_PARENTHESIZED_CONTENT.matcher(rawTitle);
    return matcher.matches() && TitleFragment.from(matcher.group(1)).matches(answer);
  }

  /** Title fragments for cloze masking. */
  public List<TitleFragment> getClozeTitleFragments() {
    return List.of(titleFragment());
  }

  private TitleFragment titleFragment() {
    return TitleFragment.from(rawTitle);
  }
}
