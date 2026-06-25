package com.odde.doughnut.algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses note titles with grammar {@code title[／alias]*[(qualifier)]?}.
 *
 * <ul>
 *   <li>{@code ／} (U+FF0F) separates aliases; {@code ／／} is a literal {@code ／}; ASCII {@code /}
 *       never separates.
 *   <li>Any leading {@code ~}/{@code 〜}/{@code ～} on a title alias or the qualifier marks a cloze
 *       suffix fragment.
 *   <li>Only the last trailing bracket pair (any Unicode {@code \p{Ps}…\p{Pe}}) is the qualifier;
 *       the qualifier is a single value (no aliases).
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

  /**
   * Spelling recall: primary stem plus {@code ~} suffix fragments only (not plain title aliases).
   */
  public boolean matchesForRecall(String answer) {
    if (rawTitle.trim().equalsIgnoreCase(answer)) {
      return true;
    }
    return getRecallTitleFragments().stream().anyMatch(t -> t.matches(answer));
  }

  /**
   * Title fragments for recall answer matching and cloze masking: primary plus {@code ~} suffix
   * fragments. Plain title aliases after the first {@code ／} segment are excluded.
   */
  public List<TitleFragment> getRecallTitleFragments() {
    TitleAliasMigrationPlan.Result plan = TitleAliasMigrationPlan.from(rawTitle);
    List<TitleFragment> recallFragments = new ArrayList<>();
    recallFragments.add(plan.primary());
    for (String suffixStem : plan.retainedSuffixFragments()) {
      recallFragments.add(TitleFragment.from("~" + suffixStem));
    }
    return TitleFragment.sortedLongestFirst(recallFragments);
  }

  /** Alias segments in title order (primary first); not sorted by length. */
  public List<TitleFragment> getAliasSegmentsInOrder() {
    if (parsedSections.aliasSection() == null) {
      return List.of();
    }
    return splitAliases(parsedSections.aliasSection());
  }

  public Optional<TitleFragment> getQualifier() {
    return Optional.ofNullable(parsedSections.qualifierSection()).map(TitleFragment::from);
  }

  private static ParsedSections parseSections(String rawTitle) {
    Matcher matcher = TITLE_WITH_QUALIFIER.matcher(rawTitle);
    if (!matcher.find()) {
      return new ParsedSections(null, null);
    }
    return new ParsedSections(matcher.group(1), matcher.group(3));
  }

  private static List<TitleFragment> splitAliases(String text) {
    List<String> rawSegments = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    for (int i = 0; i < text.length(); i++) {
      char character = text.charAt(i);
      if (character == '／') {
        if (i + 1 < text.length() && text.charAt(i + 1) == '／') {
          current.append('／');
          i++;
        } else {
          rawSegments.add(current.toString());
          current = new StringBuilder();
        }
      } else {
        current.append(character);
      }
    }
    rawSegments.add(current.toString());
    return rawSegments.stream().map(TitleFragment::from).toList();
  }

  private record ParsedSections(String aliasSection, String qualifierSection) {}
}
