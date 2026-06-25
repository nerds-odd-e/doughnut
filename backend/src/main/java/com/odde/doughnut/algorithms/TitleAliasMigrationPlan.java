package com.odde.doughnut.algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses title segments for recall: primary, plain aliases, {@code ~} suffix fragments, qualifier.
 */
public final class TitleAliasMigrationPlan {

  private TitleAliasMigrationPlan() {}

  public record Result(
      TitleFragment primary,
      List<String> plainAliases,
      List<String> retainedSuffixFragments,
      Optional<String> qualifier) {}

  public static Result from(String rawTitle) {
    NoteTitle noteTitle = new NoteTitle(rawTitle);
    List<TitleFragment> segments = noteTitle.getAliasSegmentsInOrder();
    if (segments.isEmpty()) {
      return new Result(
          TitleFragment.from(""),
          List.of(),
          List.of(),
          noteTitle.getQualifier().map(TitleFragment::stem));
    }

    TitleFragment primary = segments.getFirst();
    List<String> plainAliases = new ArrayList<>();
    List<String> retainedSuffixFragments = new ArrayList<>();
    for (int i = 1; i < segments.size(); i++) {
      TitleFragment segment = segments.get(i);
      if (segment.suffixMarker()) {
        retainedSuffixFragments.add(segment.stem());
      } else {
        plainAliases.add(segment.stem());
      }
    }

    return new Result(
        primary,
        List.copyOf(plainAliases),
        List.copyOf(retainedSuffixFragments),
        noteTitle.getQualifier().map(TitleFragment::stem));
  }
}
