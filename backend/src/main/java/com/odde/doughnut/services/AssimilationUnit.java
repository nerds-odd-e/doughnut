package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import java.util.Comparator;

/** A note-level or property-level item in the assimilation queue. */
public record AssimilationUnit(Note note, String propertyKey) {

  public static final Comparator<AssimilationUnit> ORDER =
      Comparator.comparing((AssimilationUnit unit) -> unit.note().getRecallSetting().getLevel())
          .thenComparing(unit -> unit.note().getCreatedAt())
          .thenComparing(unit -> unit.note().getId())
          .thenComparing(AssimilationUnit::isPropertyLevel)
          .thenComparing(
              unit -> unit.propertyKey() == null ? "" : unit.propertyKey(),
              String.CASE_INSENSITIVE_ORDER);

  public static AssimilationUnit forNote(Note note) {
    return new AssimilationUnit(note, null);
  }

  public static AssimilationUnit forProperty(Note note, String propertyKey) {
    return new AssimilationUnit(note, propertyKey);
  }

  public boolean isPropertyLevel() {
    return propertyKey != null && !propertyKey.isEmpty();
  }
}
