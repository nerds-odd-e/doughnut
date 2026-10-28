package com.odde.donut.entities;

/** Test-only access to assign a tracker id so Grade fuzz matches a persisted identity. */
public final class MemoryTrackerTestIds {
  private MemoryTrackerTestIds() {}

  public static void assign(MemoryTracker tracker, Integer id) {
    tracker.id = id;
  }
}
