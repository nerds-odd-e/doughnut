package com.odde.doughnut.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class NoteTitle {

  private final String title;

  public NoteTitle(String title) {
    this.title = title;
  }

  public boolean matches(String answer) {
    if (title.trim().equalsIgnoreCase(answer)) {
      return true;
    }
    return getTitles().stream().anyMatch(t -> t.matches(answer));
  }

  String replaceTitleFragments(String pronunciationMasked, ClozeReplacement clozeReplacement) {
    String literalMatchPreMasked =
        getTitles().stream()
            .reduce(pronunciationMasked, (d, t) -> t.replaceLiteralWords(d), (x, y) -> y);
    String titlePreMasked =
        getTitles().stream()
            .reduce(literalMatchPreMasked, (d, t) -> t.replaceSimilar(d), (x, y) -> y);
    return clozeReplacement.replaceMasks(titlePreMasked);
  }

  private List<TitleFragment> getTitles() {
    List<TitleFragment> result = new ArrayList<>();
    Pattern pattern = Pattern.compile("(?U)(.+?)(\\p{Ps}([^\\p{Ps}\\p{Pe}]+)\\p{Pe})?$");
    Matcher matcher = pattern.matcher(title);
    if (matcher.find()) {
      getFragments(matcher.group(1), false).forEach(result::add);
      getFragments(matcher.group(3), true).forEach(result::add);
    }
    result.sort(Comparator.comparing(TitleFragment::length));
    Collections.reverse(result);
    return result;
  }

  private Stream<TitleFragment> getFragments(String subString, boolean subtitle) {
    return Arrays.stream(subString != null ? subString.split("(?<!/)[/／](?!/)") : new String[] {})
        .map(s -> new TitleFragment(s, subtitle));
  }
}
