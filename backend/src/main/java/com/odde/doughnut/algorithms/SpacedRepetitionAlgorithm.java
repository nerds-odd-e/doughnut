package com.odde.doughnut.algorithms;

import java.util.Arrays;
import java.util.List;

/**
 * Built-in spacing ladder in days (Fibonacci) for converting legacy index rows. Not a per-user
 * table.
 */
public final class SpacedRepetitionAlgorithm {
  private static final List<Integer> DEFAULT_SPACES =
      Arrays.asList(
          0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377, 610, 987, 1597, 2584, 4181, 6765,
          10946, 17711, 28657, 46368, 75025);

  /** Historical index scale used only to convert existing rows. */
  private static final float LEGACY_INDEX_OFFSET = 100.0f;

  public static final float LEGACY_INDEX_STEP = 10.0f;

  private SpacedRepetitionAlgorithm() {}

  /**
   * Convert a legacy forgetting-curve index and optional per-user day list to hours. Empty list
   * uses {@link #DEFAULT_SPACES}.
   */
  public static int hoursFromLegacyIndex(float legacyIndex, List<Integer> spaces) {
    float spacingIndex = (legacyIndex - LEGACY_INDEX_OFFSET) / LEGACY_INDEX_STEP;
    return hoursFromSpacingIndex(spacingIndex, spaces);
  }

  private static int hoursFromSpacingIndex(float spacingIndex, List<Integer> spaces) {
    if (spacingIndex < 0) {
      return 0;
    }
    int floor = spacingDays((int) spacingIndex, spaces);
    int ceiling = spacingDays((int) spacingIndex + 1, spaces);
    return (int) (floor * 24 + (ceiling - floor) * 24 * (spacingIndex - (int) spacingIndex));
  }

  private static int spacingDays(int index, List<Integer> spaces) {
    if (index < spaces.size()) {
      return spaces.get(index);
    }
    int defaultIndex = Math.min(index, DEFAULT_SPACES.size() - 1);
    return DEFAULT_SPACES.get(defaultIndex);
  }
}
