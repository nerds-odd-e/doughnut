package com.odde.donut.services;

import com.odde.donut.entities.Note;
import java.sql.Timestamp;
import java.util.Comparator;

/**
 * A note-level or property-level item in the assimilation queue. A property unit is one value of a
 * property: {@code propertyValue} is the list item, or {@code ""} for a single (scalar) value.
 * {@code time} orders units within a level: a note unit's creation time, or a property value's
 * change date (its note's creation time when undated), both as stored, so that units from separate
 * queries compare as each query ordered them.
 */
public record AssimilationUnit(
    Note note, String propertyKey, String propertyValue, int level, Timestamp time) {

  public static final Comparator<AssimilationUnit> ORDER =
      Comparator.comparingInt(AssimilationUnit::level)
          .thenComparing(AssimilationUnit::time)
          .thenComparing(unit -> unit.note().getId())
          .thenComparing(AssimilationUnit::isPropertyLevel)
          .thenComparing(
              unit -> unit.propertyKey() == null ? "" : unit.propertyKey(),
              String.CASE_INSENSITIVE_ORDER);

  /** JPQL: note unit at its stored creation time; missing cache row is 0. */
  public AssimilationUnit(Note note, Number cachedLevel, Timestamp time) {
    this(note, null, null, levelOf(cachedLevel), time);
  }

  /** JPQL: property value unit at its stored (coalesced) change time; missing cache row is 0. */
  public AssimilationUnit(
      Note note, String propertyKey, String propertyValue, Number cachedLevel, Timestamp time) {
    this(note, propertyKey, propertyValue, levelOf(cachedLevel), time);
  }

  private static int levelOf(Number cachedLevel) {
    return cachedLevel == null ? 0 : cachedLevel.intValue();
  }

  public boolean isPropertyLevel() {
    return propertyKey != null && !propertyKey.isEmpty();
  }
}
