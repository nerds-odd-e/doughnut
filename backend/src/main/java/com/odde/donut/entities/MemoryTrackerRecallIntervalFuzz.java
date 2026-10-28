package com.odde.donut.entities;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.zip.CRC32;

/**
 * Anki-scaled whole-hour review-interval fuzz for Grade scheduling. Stability stays an FSRS output;
 * the fuzzed hour interval is chosen from durable Grade-event fields so replay rebuilds match.
 */
final class MemoryTrackerRecallIntervalFuzz {
  static final float THRESHOLD_HOURS = 60f;

  private static final float BASE_DELTA_HOURS = 24f;
  private static final float[][] RANGES = {
    {THRESHOLD_HOURS, 168f, 0.15f},
    {168f, 480f, 0.10f},
    {480f, Float.MAX_VALUE, 0.05f}
  };

  private MemoryTrackerRecallIntervalFuzz() {}

  /**
   * Whole-hour Grade interval from durable event fields. Below {@link #THRESHOLD_HOURS} returns the
   * unfuzzed Stability interval; at or above, applies Anki-scaled review fuzz.
   */
  static int intervalHoursForGrade(
      Integer memoryTrackerId,
      Timestamp recordedAt,
      float stabilityHours,
      int minimum,
      int maximum) {
    if (stabilityHours < THRESHOLD_HOURS) {
      return Math.clamp(Fsrs.intervalHours(stabilityHours), minimum, maximum);
    }
    return withReviewFuzz(
        factorFrom(memoryTrackerId, recordedAt), stabilityHours, minimum, maximum);
  }

  /**
   * Uniform pick inside constrained integer-hour bounds; {@code fuzzFactor} is in {@code [0, 1)}.
   */
  private static int withReviewFuzz(
      float fuzzFactor, float intervalHours, int minimum, int maximum) {
    int[] bounds = constrainedFuzzBounds(intervalHours, minimum, maximum);
    return (int) Math.floor(bounds[0] + fuzzFactor * (1 + bounds[1] - bounds[0]));
  }

  static int[] constrainedFuzzBounds(float intervalHours, int minimum, int maximum) {
    minimum = Math.min(minimum, maximum);
    float clamped = Math.max(minimum, Math.min(maximum, intervalHours));
    float delta = fuzzDelta(clamped);
    int lower = Math.clamp(Math.round(clamped - delta), minimum, maximum);
    int upper = Math.clamp(Math.round(clamped + delta), minimum, maximum);
    if (upper == lower && upper > 2 && upper < maximum) {
      upper = lower + 1;
    }
    return new int[] {lower, upper};
  }

  private static float fuzzDelta(float intervalHours) {
    if (intervalHours < THRESHOLD_HOURS) {
      return 0f;
    }
    float delta = BASE_DELTA_HOURS;
    for (float[] range : RANGES) {
      delta += range[2] * Math.max(0f, Math.min(intervalHours, range[1]) - range[0]);
    }
    return delta;
  }

  /** Deterministic {@code [0, 1)} factor from durable RecallLog Grade-event fields. */
  private static float factorFrom(Integer memoryTrackerId, Timestamp recordedAt) {
    CRC32 crc32 = new CRC32();
    crc32.update(
        (String.valueOf(memoryTrackerId) + Long.toString(recordedAt.getTime()))
            .getBytes(StandardCharsets.UTF_8));
    return (crc32.getValue() & 0xffffffffL) / 4294967296f;
  }
}
