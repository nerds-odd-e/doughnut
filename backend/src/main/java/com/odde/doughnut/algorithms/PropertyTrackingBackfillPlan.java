package com.odde.doughnut.algorithms;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Pure planner for note property index rows and skipped property-tracker seeding. */
public final class PropertyTrackingBackfillPlan {

  private PropertyTrackingBackfillPlan() {}

  public record Result(Set<String> keysToIndex, Set<String> keysToSeedSkipped) {}

  /**
   * Decides which frontmatter keys to index and which indexed keys should receive a skipped memory
   * tracker when none exists yet (case-insensitive on property keys).
   */
  public static Result forNote(
      Frontmatter frontmatter, Set<String> existingNonDeletedPropertyKeys) {
    return forPlannedRows(
        NotePropertyIndexPlanner.plannedRows(frontmatter), existingNonDeletedPropertyKeys);
  }

  public static Result forPlannedRows(
      List<NotePropertyIndexPlanner.PlannedRow> plannedRows,
      Set<String> existingNonDeletedPropertyKeys) {
    Set<String> keysToIndex = new LinkedHashSet<>();
    for (NotePropertyIndexPlanner.PlannedRow row : plannedRows) {
      keysToIndex.add(row.propertyKey());
    }
    Set<String> keysToSeedSkipped = new LinkedHashSet<>();
    for (String key : keysToIndex) {
      if (!PropertyKeyNaming.isExampleOfPropertyKey(key)
          && !isAlreadyTracked(key, existingNonDeletedPropertyKeys)) {
        keysToSeedSkipped.add(key);
      }
    }
    return new Result(Set.copyOf(keysToIndex), Set.copyOf(keysToSeedSkipped));
  }

  private static boolean isAlreadyTracked(String key, Set<String> existingNonDeletedPropertyKeys) {
    for (String existing : existingNonDeletedPropertyKeys) {
      if (existing != null && existing.equalsIgnoreCase(key)) {
        return true;
      }
    }
    return false;
  }
}
