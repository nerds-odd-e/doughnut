package com.odde.doughnut.algorithms;

import java.util.LinkedHashSet;
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
      Set<String> frontmatterKeys, Set<String> existingNonDeletedPropertyKeys) {
    Set<String> keysToIndex = new LinkedHashSet<>();
    Set<String> keysToSeedSkipped = new LinkedHashSet<>();
    for (String key : frontmatterKeys) {
      if (key == null || key.isBlank()) {
        continue;
      }
      if (PropertyKeyNaming.isReservedStructuralKey(key)) {
        continue;
      }
      keysToIndex.add(key);
      if (!PropertyKeyNaming.isExampleOfFamily(key)
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
