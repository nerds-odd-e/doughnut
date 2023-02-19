package com.odde.doughnut.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

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
    List<TitleFragment> result = new ArrayList<>();
    Matcher matcher = titleParts();
    getFragments(matcher.group(1), false).forEach(result::add);
    getFragments(matcher.group(3), true).forEach(result::add);
    result.sort(Comparator.comparing(TitleFragment::length));
    Collections.reverse(result);
    return result;
  }

  @NotNull
  private Matcher titleParts() {
    Pattern pattern = Pattern.compile("(?U)(.+?)(\\p{Ps}([^\\p{Ps}\\p{Pe}]+)\\p{Pe})?$");
    Matcher matcher = pattern.matcher(title);
    matcher.find();
    return matcher;
  }

  private Stream<TitleFragment> getFragments(String subString, boolean subtitle) {
    return Arrays.stream(subString != null ? subString.split("(?<!/)[/／](?!/)") : new String[] {})
        .map(s -> new TitleFragment(s, subtitle));
  }

  public List<TitleFragment> getSubtitles() {
    Matcher matcher = titleParts();
    List<TitleFragment> result = new ArrayList<>();
    getFragments(matcher.group(3), true).forEach(result::add);
    result.sort(Comparator.comparing(TitleFragment::length));
    Collections.reverse(result);
    return result;
  }
}
