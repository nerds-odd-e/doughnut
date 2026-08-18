package com.odde.doughnut.entities;

final class MemoryTrackerNextStability {
  private MemoryTrackerNextStability() {}

  static void write(MemoryTracker tracker, float nextStability) {
    tracker.setStability(Fsrs.cappedStabilityHours(nextStability));
  }
}
