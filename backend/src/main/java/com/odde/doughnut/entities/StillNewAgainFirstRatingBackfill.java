package com.odde.doughnut.entities;

import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public final class StillNewAgainFirstRatingBackfill {

  private StillNewAgainFirstRatingBackfill() {}

  public static void run(Connection connection, String gate) throws SQLException {
    if ("1=0".equals(gate)) {
      return;
    }
    if (!"1=1".equals(gate)) {
      throw new IllegalStateException(
          "Still-New Again first-rating backfill gate must be 1=0 or 1=1, got: " + gate);
    }

    ForgettingCurve.NextMemory firstAgain =
        new ForgettingCurve(ForgettingCurve.ASSIMILATE_STABILITY_HOURS).afterAgainRecall(0);
    float stability = firstAgain.stability();
    float difficulty = firstAgain.difficulty();
    int intervalHours = Fsrs.intervalHours(stability);

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
                      AND rl.product_outcome IN (?, ?)
                  )
                """);
        PreparedStatement update =
            connection.prepareStatement(
                """
                UPDATE memory_tracker
                SET stability = ?, difficulty = ?, next_recall_at = ?
                WHERE id = ?
                """)) {
      select.setString(1, ProductOutcome.AGAIN.name());
      select.setString(2, ProductOutcome.AGAIN_ZERO.name());
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
