package com.odde.doughnut.entities;

import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;

public final class StillNewFirstRatingBackfill {

  private StillNewFirstRatingBackfill() {}

  public static void runAgain(Connection connection, String gate) throws SQLException {
    ForgettingCurve.NextMemory firstAgain =
        new ForgettingCurve(ForgettingCurve.ASSIMILATE_STABILITY_HOURS).afterAgainRecall(0);
    run(connection, gate, firstAgain, ProductOutcome.AGAIN, ProductOutcome.AGAIN_ZERO);
  }

  public static void runHard(Connection connection, String gate) throws SQLException {
    ForgettingCurve.NextMemory firstHard =
        new ForgettingCurve(ForgettingCurve.ASSIMILATE_STABILITY_HOURS).afterHardRecall(0);
    run(connection, gate, firstHard, ProductOutcome.SHRINK);
  }

  private static void run(
      Connection connection,
      String gate,
      ForgettingCurve.NextMemory first,
      ProductOutcome... outcomes)
      throws SQLException {
    if ("1=0".equals(gate)) {
      return;
    }
    if (!"1=1".equals(gate)) {
      throw new IllegalStateException(
          "Still-New first-rating backfill gate must be 1=0 or 1=1, got: " + gate);
    }

    float stability = first.stability();
    float difficulty = first.difficulty();
    int intervalHours = Fsrs.intervalHours(stability);
    String inList = String.join(", ", Collections.nCopies(outcomes.length, "?"));

    try (PreparedStatement select =
            connection.prepareStatement(
                """
                SELECT mt.id, mt.last_recalled_at
                FROM memory_tracker mt
                WHERE mt.stability = 0
                  AND mt.difficulty IS NULL
                  AND EXISTS (
                    SELECT 1 FROM recall_log rl
                    WHERE rl.memory_tracker_id = mt.id
                      AND rl.product_outcome IN (%s)
                  )
                """
                    .formatted(inList));
        PreparedStatement update =
            connection.prepareStatement(
                """
                UPDATE memory_tracker
                SET stability = ?, difficulty = ?, next_recall_at = ?
                WHERE id = ?
                """)) {
      for (int i = 0; i < outcomes.length; i++) {
        select.setString(i + 1, outcomes[i].name());
      }
      try (ResultSet rows = select.executeQuery()) {
        boolean pending = false;
        while (rows.next()) {
          Timestamp nextRecallAt =
              TimestampOperations.addHoursToTimestamp(
                  rows.getTimestamp("last_recalled_at"), intervalHours);
          update.setFloat(1, stability);
          update.setFloat(2, difficulty);
          update.setTimestamp(3, nextRecallAt);
          update.setInt(4, rows.getInt("id"));
          update.addBatch();
          pending = true;
        }
        if (pending) {
          update.executeBatch();
        }
      }
    }
  }
}
