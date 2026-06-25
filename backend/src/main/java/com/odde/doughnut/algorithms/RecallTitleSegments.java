package com.odde.doughnut.algorithms;

import java.util.ArrayList;
import java.util.List;

/** Parses recall title segments: primary stem plus {@code ~} suffix fragments. */
public final class RecallTitleSegments {

  private RecallTitleSegments() {}

  public record Result(TitleFragment primary, List<String> retainedSuffixFragments) {}

  public static Result from(String rawTitle) {
    NoteTitle noteTitle = new NoteTitle(rawTitle);
    List<TitleFragment> segments = noteTitle.getAliasSegmentsInOrder();
    if (segments.isEmpty()) {
      return new Result(TitleFragment.from(""), List.of());
    }

    TitleFragment primary = segments.getFirst();
    List<String> retainedSuffixFragments = new ArrayList<>();
    for (int i = 1; i < segments.size(); i++) {
      TitleFragment segment = segments.get(i);
      if (segment.suffixMarker()) {
        retainedSuffixFragments.add(segment.stem());
      }
    }

    return new Result(primary, List.copyOf(retainedSuffixFragments));
  }
}
