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

  public List<TitleFragment> getTitles() {
    return getTitleFragments(false);
  }

  private List<TitleFragment> getTitleFragments(boolean subtitle) {
    List<TitleFragment> result = new ArrayList<>();
    Pattern pattern = Pattern.compile("(?U)(.+?)(\\p{Ps}([^\\p{Ps}\\p{Pe}]+)\\p{Pe})?$");
    Matcher matcher = pattern.matcher(title);
    if (matcher.find()) {
      getFragments(matcher.group(subtitle ? 3 : 1)).forEach(result::add);
    }
    result.sort(Comparator.comparing(TitleFragment::length));
    Collections.reverse(result);
    return result;
  }

  private Stream<TitleFragment> getFragments(String subString) {
    return Arrays.stream(subString != null ? subString.split("(?<!/)[/／](?!/)") : new String[] {})
        .map(TitleFragment::new);
  }

  public List<TitleFragment> getSubtitles() {
    return getTitleFragments(true);
  }
}
