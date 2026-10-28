package com.odde.donut.entities;

import java.sql.Timestamp;

final class MemoryTrackerGrade {
  private MemoryTrackerGrade() {}

  static void apply(MemoryTracker tracker, Timestamp now, Grade grade) {
    long elapsed = tracker.elapsedHoursUntil(now);
    Float stability = tracker.getStability();
    Float difficulty = tracker.getDifficulty();
    Fsrs.NextMemory next =
        switch (grade) {
          case GOOD -> Fsrs.afterGoodRecall(stability, difficulty, elapsed);
          case EASY -> Fsrs.afterEasyRecall(stability, difficulty, elapsed);
          case HARD -> Fsrs.afterHardRecall(stability, difficulty, elapsed);
          case AGAIN -> Fsrs.afterAgainRecall(stability, difficulty, elapsed);
        };
    tracker.setDifficulty(next.difficulty());
    MemoryTrackerNextStability.write(tracker, next.stability());
    tracker.scheduleNextRecallAt(now);
  }
}
