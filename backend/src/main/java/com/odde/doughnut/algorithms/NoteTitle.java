package com.odde.doughnut.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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

  /** Only U+FF0F separates aliases; double {@code ／／} is one literal {@code ／}. */
  private static final String ALIAS_SEPARATOR = "(?<![／])[／](?![／])";

  private final String rawTitle;
  private final ParsedSections parsedSections;

  public NoteTitle(String rawTitle) {
    this.rawTitle = rawTitle;
    this.parsedSections = parseSections(rawTitle);
  }

  public boolean matches(String answer) {
    if (rawTitle.trim().equalsIgnoreCase(answer)) {
      return true;
    }
    return getTitleAliases().stream().anyMatch(t -> t.matches(answer));
  }

  public List<TitleFragment> getTitleAliases() {
    List<TitleFragment> result = new ArrayList<>();
    if (parsedSections.aliasSection() != null) {
      splitAliases(parsedSections.aliasSection()).forEach(result::add);
    }
    result.sort(Comparator.comparing(TitleFragment::length));
    Collections.reverse(result);
    return result;
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
    return Arrays.stream(text.split(ALIAS_SEPARATOR)).map(TitleFragment::from).toList();
  }

  private record ParsedSections(String aliasSection, String qualifierSection) {}
}
