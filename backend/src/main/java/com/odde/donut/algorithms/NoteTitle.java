package com.odde.donut.algorithms;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A title has one list of fragments: the whole title text as one literal fragment, then, when the
 * title ends with round-parenthesized content, the text before it if any. Recall matching accepts
 * an answer that matches any fragment, and cloze masking hides every fragment. A {@code ~}/{@code
 * 〜}/{@code ～} marker at the actual beginning still marks a suffix fragment.
 */
public class NoteTitle {

  private static final Pattern TITLE_WITH_TRAILING_PARENTHESIZED_CONTENT =
      Pattern.compile("(?s)(.+)\\([^()]+\\)\\s*$");

  private final String rawTitle;

  public NoteTitle(String rawTitle) {
    this.rawTitle = rawTitle;
  }

  public boolean matchesForRecall(String answer) {
    return getTitleFragments().stream().anyMatch(fragment -> fragment.matches(answer));
  }

  public List<TitleFragment> getTitleFragments() {
    TitleFragment wholeTitle = TitleFragment.from(rawTitle);
    Matcher matcher = TITLE_WITH_TRAILING_PARENTHESIZED_CONTENT.matcher(rawTitle);
    if (!matcher.matches()) {
      return List.of(wholeTitle);
    }
    TitleFragment beforeQualifier = TitleFragment.from(matcher.group(1));
    if (beforeQualifier.stem().isEmpty()) {
      return List.of(wholeTitle);
    }
    return List.of(wholeTitle, beforeQualifier);
  }
}
