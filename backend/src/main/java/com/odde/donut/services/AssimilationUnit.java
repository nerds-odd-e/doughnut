package com.odde.donut.services;

import com.odde.donut.entities.Note;
import java.util.Comparator;

/** A note-level or property-level item in the assimilation queue. */
public record AssimilationUnit(Note note, String propertyKey, int level) {

  public static final Comparator<AssimilationUnit> ORDER =
      Comparator.comparingInt(AssimilationUnit::level)
          .thenComparing(unit -> unit.note().getCreatedAt())
          .thenComparing(unit -> unit.note().getId())
          .thenComparing(AssimilationUnit::isPropertyLevel)
          .thenComparing(
              unit -> unit.propertyKey() == null ? "" : unit.propertyKey(),
              String.CASE_INSENSITIVE_ORDER);

  /** JPQL: note unit; missing cache row is 0. */
  public AssimilationUnit(Note note, Number cachedLevel) {
    this(note, null, cachedLevel);
  }

  /** JPQL: property unit; missing cache row is 0. */
  public AssimilationUnit(Note note, String propertyKey, Number cachedLevel) {
    this(note, propertyKey, cachedLevel == null ? 0 : cachedLevel.intValue());
  }

  public boolean isPropertyLevel() {
    return propertyKey != null && !propertyKey.isEmpty();
  }
}
