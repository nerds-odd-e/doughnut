package com.odde.doughnut.algorithms;

import java.util.ArrayList;
import java.util.List;

/** Parses recall title text into literal title text plus explicit {@code ~} suffix fragments. */
public final class RecallTitleSegments {

  private RecallTitleSegments() {}

  public record Result(TitleFragment primary, List<String> retainedSuffixFragments) {}

  public static Result from(String rawTitle) {
    return new NoteTitle(rawTitle).recallTitleSegments();
  }

  static Result fromTitleText(String titleText) {
    if (titleText == null || titleText.isEmpty()) {
      return new Result(TitleFragment.from(""), List.of());
    }

    List<String> literalSegments = new ArrayList<>();
    List<String> retainedSuffixFragments = new ArrayList<>();
    String[] segments = titleText.split("／", -1);
    literalSegments.add(segments[0]);
    for (int i = 1; i < segments.length; i++) {
      String segment = segments[i];
      TitleFragment fragment = TitleFragment.from(segment);
      if (fragment.suffixMarker()) {
        retainedSuffixFragments.add(fragment.stem());
      } else {
        literalSegments.add(segment);
      }
    }

    String primaryTitleText = String.join("／", literalSegments);
    return new Result(TitleFragment.from(primaryTitleText), List.copyOf(retainedSuffixFragments));
  }
}
