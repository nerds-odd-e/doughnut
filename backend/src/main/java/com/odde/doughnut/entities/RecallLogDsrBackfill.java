package com.odde.doughnut.entities;

import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public final class RecallLogDsrBackfill {

  private RecallLogDsrBackfill() {}

  public static void run(Connection connection) throws SQLException {
    List<FoldedSnapshot> snapshots = foldMappedTrackers(connection);
    try (PreparedStatement update =
        connection.prepareStatement(
            """
            UPDATE memory_tracker
            SET stability = ?, difficulty = ?, last_recalled_at = ?, next_recall_at = ?
            WHERE id = ?
            """)) {
      boolean pending = false;
      for (FoldedSnapshot snapshot : snapshots) {
        update.setFloat(1, snapshot.stability());
        update.setFloat(2, snapshot.difficulty());
        update.setTimestamp(3, snapshot.lastRecalledAt());
        update.setTimestamp(4, snapshot.nextRecallAt());
        update.setInt(5, snapshot.trackerId());
        update.addBatch();
        pending = true;
      }
      if (pending) {
        update.executeBatch();
      }
    }
  }

  private static List<FoldedSnapshot> foldMappedTrackers(Connection connection)
      throws SQLException {
    List<FoldedSnapshot> snapshots = new ArrayList<>();
    try (PreparedStatement select =
            connection.prepareStatement(
                """
                SELECT mt.id AS tracker_id,
                       rl.recorded_at,
                       rl.product_outcome,
                       rl.elapsed_hours
                FROM memory_tracker mt
                JOIN recall_log rl ON rl.memory_tracker_id = mt.id
                WHERE mt.deleted_at IS NULL
                  AND EXISTS (
                    SELECT 1 FROM recall_log mapped
                    WHERE mapped.memory_tracker_id = mt.id
                      AND mapped.product_outcome IN (%s)
                  )
                ORDER BY mt.id, rl.recorded_at, rl.id
                """
                    .formatted(ProductOutcome.mappedGradeSqlInList()));
        ResultSet rows = select.executeQuery()) {
      int currentTrackerId = 0;
      DsrFold fold = null;
      while (rows.next()) {
        int trackerId = rows.getInt("tracker_id");
        if (fold == null || trackerId != currentTrackerId) {
          if (fold != null) {
            snapshots.add(fold.snapshot(currentTrackerId));
          }
          currentTrackerId = trackerId;
          fold = new DsrFold();
        }
        fold.apply(
            ProductOutcome.valueOf(rows.getString("product_outcome")),
            rows.getTimestamp("recorded_at"),
            rows.getInt("elapsed_hours"));
      }
      if (fold != null) {
        snapshots.add(fold.snapshot(currentTrackerId));
      }
    }
    return snapshots;
  }

  private static Timestamp dueAt(Timestamp lastRecalledAt, float stabilityHours) {
    int intervalHours = Fsrs.intervalHours(stabilityHours);
    if (intervalHours <= 0) {
      intervalHours = Fsrs.intervalHours(Fsrs.STRICTLY_FUTURE_FALLBACK_HOURS);
    }
    return TimestampOperations.addHoursToTimestamp(lastRecalledAt, intervalHours);
  }

  private static Fsrs.NextMemory afterMappedGrade(
      ProductOutcome outcome, float stability, Float difficulty, long elapsedHours) {
    return switch (outcome) {
      case GOOD -> Fsrs.afterGoodRecall(stability, difficulty, elapsedHours);
      case EASY -> Fsrs.afterEasyRecall(stability, difficulty, elapsedHours);
      case HARD -> Fsrs.afterHardRecall(stability, difficulty, elapsedHours);
      case AGAIN -> Fsrs.afterAgainRecall(stability, difficulty, elapsedHours);
      default -> throw new IllegalStateException("not a mapped grade: " + outcome);
    };
  }

  private static final class DsrFold {
    private float stability = Fsrs.NEW_STABILITY_HOURS;
    private Float difficulty;
    private Timestamp lastRecalledAt;
    private Timestamp nextRecallAt;

    void apply(ProductOutcome outcome, Timestamp recordedAt, int elapsedHours) {
      if (outcome == ProductOutcome.CONFUSION) {
        applyConfusion(elapsedHours);
        return;
      }
      Fsrs.NextMemory next = afterMappedGrade(outcome, stability, difficulty, elapsedHours);
      stability = Fsrs.cappedStabilityHours(next.stability());
      difficulty = next.difficulty();
      lastRecalledAt = recordedAt;
      nextRecallAt = dueAt(recordedAt, stability);
    }

    private void applyConfusion(int elapsedHours) {
      stability =
          Fsrs.cappedStabilityHours(Fsrs.confusionAdjusted(stability, difficulty, elapsedHours));
      if (lastRecalledAt == null) {
        return;
      }
      Timestamp projected =
          TimestampOperations.addHoursToTimestamp(lastRecalledAt, Fsrs.intervalHours(stability));
      if (!projected.after(nextRecallAt)) {
        nextRecallAt = projected;
      }
    }

    FoldedSnapshot snapshot(int trackerId) {
      if (lastRecalledAt == null || nextRecallAt == null || difficulty == null) {
        throw new IllegalStateException("mapped-grade tracker folded without a mapped grade");
      }
      return new FoldedSnapshot(trackerId, stability, difficulty, lastRecalledAt, nextRecallAt);
    }
  }

  private record FoldedSnapshot(
      int trackerId,
      float stability,
      float difficulty,
      Timestamp lastRecalledAt,
      Timestamp nextRecallAt) {}
}
