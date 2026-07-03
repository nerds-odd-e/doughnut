package com.odde.doughnut.algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses note titles into the literal title text, explicit cloze suffix fragments, and an optional
 * qualifier.
 *
 * <ul>
 *   <li>{@code ／} (U+FF0F) is a literal title character unless the following segment starts with
 *       {@code ~}/{@code 〜}/{@code ～}.
 *   <li>Marked suffix fragments participate in recall matching and cloze masking.
 *   <li>Only the last trailing bracket pair (any Unicode {@code \p{Ps}…\p{Pe}}) is the qualifier;
 *       the qualifier is a single value.
 * </ul>
 */
public class NoteTitle {

  private static final Pattern TITLE_WITH_QUALIFIER =
      Pattern.compile("(?U)(.+?)(\\p{Ps}([^\\p{Ps}\\p{Pe}]+)\\p{Pe})?$");

  private final String rawTitle;
  private final ParsedSections parsedSections;

  public NoteTitle(String rawTitle) {
    this.rawTitle = rawTitle;
    this.parsedSections = parseSections(rawTitle);
  }

  public boolean matchesForRecall(String answer) {
    if (rawTitle.trim().equalsIgnoreCase(answer)) {
      return true;
    }
    return getRecallTitleFragments().stream().anyMatch(t -> t.matches(answer));
  }

  /** Title fragments for recall answer matching and cloze masking. */
  public List<TitleFragment> getRecallTitleFragments() {
    RecallTitleSegments.Result segments = recallTitleSegments();
    List<TitleFragment> recallFragments = new ArrayList<>();
    recallFragments.add(segments.primary());
    for (String suffixStem : segments.retainedSuffixFragments()) {
      recallFragments.add(TitleFragment.from("~" + suffixStem));
    }
    return TitleFragment.sortedLongestFirst(recallFragments);
  }

  public Optional<TitleFragment> getQualifier() {
    return Optional.ofNullable(parsedSections.qualifierSection()).map(TitleFragment::from);
  }

  private String titleText() {
    return parsedSections.titleSection() == null ? rawTitle : parsedSections.titleSection();
  }

  RecallTitleSegments.Result recallTitleSegments() {
    return RecallTitleSegments.fromTitleText(titleText());
  }

  private static ParsedSections parseSections(String rawTitle) {
    Matcher matcher = TITLE_WITH_QUALIFIER.matcher(rawTitle);
    if (!matcher.find()) {
      return new ParsedSections(null, null);
    }
    return new ParsedSections(matcher.group(1), matcher.group(3));
  }

  private record ParsedSections(String titleSection, String qualifierSection) {}
}
