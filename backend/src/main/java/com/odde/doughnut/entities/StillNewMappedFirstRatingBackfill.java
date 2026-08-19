package com.odde.doughnut.entities;

import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public final class StillNewMappedFirstRatingBackfill {

  private StillNewMappedFirstRatingBackfill() {}

  public static void run(Connection connection) throws SQLException {
    Fsrs.NextMemory firstAgain = Fsrs.firstRating(Fsrs.AGAIN);
    Fsrs.NextMemory firstHard = Fsrs.firstRating(Fsrs.HARD);
    Fsrs.NextMemory firstGood = Fsrs.firstRating(Fsrs.GOOD);
    Fsrs.NextMemory firstEasy = Fsrs.firstRating(Fsrs.EASY);

    try (PreparedStatement select =
            connection.prepareStatement(
                """
                SELECT mt.id,
                       MAX(rl.recorded_at) AS last_mapped_at,
                       MAX(CASE WHEN rl.product_outcome IN ('AGAIN', 'AGAIN_ZERO') THEN 1 ELSE 0 END) AS has_again,
                       MAX(CASE WHEN rl.product_outcome = 'SHRINK' THEN 1 ELSE 0 END) AS has_shrink,
                       MAX(CASE WHEN rl.product_outcome = 'HARD' THEN 1 ELSE 0 END) AS has_hard,
                       MAX(CASE WHEN rl.product_outcome = 'GOOD' THEN 1 ELSE 0 END) AS has_good,
                       MAX(CASE WHEN rl.product_outcome = 'EASY' THEN 1 ELSE 0 END) AS has_easy
                FROM memory_tracker mt
                JOIN recall_log rl
                  ON rl.memory_tracker_id = mt.id
                 AND rl.product_outcome IN (
                   %s, 'SHRINK', 'AGAIN_ZERO'
                 )
                WHERE mt.stability = 0
                  AND mt.difficulty IS NULL
                GROUP BY mt.id
                """
                    .formatted(ProductOutcome.mappedGradeSqlInList()));
        PreparedStatement update =
            connection.prepareStatement(
                """
                UPDATE memory_tracker
                SET stability = ?, difficulty = ?, last_recalled_at = ?, next_recall_at = ?
                WHERE id = ?
                """)) {
      try (ResultSet rows = select.executeQuery()) {
        boolean pending = false;
        while (rows.next()) {
          Fsrs.NextMemory first =
              firstRatingForRow(rows, firstAgain, firstHard, firstGood, firstEasy);
          Timestamp lastMappedAt = rows.getTimestamp("last_mapped_at");
          update.setFloat(1, first.stability());
          update.setFloat(2, first.difficulty());
          update.setTimestamp(3, lastMappedAt);
          update.setTimestamp(
              4,
              TimestampOperations.addHoursToTimestamp(
                  lastMappedAt, Fsrs.intervalHours(first.stability())));
          update.setInt(5, rows.getInt("id"));
          update.addBatch();
          pending = true;
        }
        if (pending) {
          update.executeBatch();
        }
      }
    }
  }

  private static Fsrs.NextMemory firstRatingForRow(
      ResultSet row,
      Fsrs.NextMemory firstAgain,
      Fsrs.NextMemory firstHard,
      Fsrs.NextMemory firstGood,
      Fsrs.NextMemory firstEasy)
      throws SQLException {
    if (row.getInt("has_again") != 0) {
      return firstAgain;
    }
    if (row.getInt("has_shrink") != 0 || row.getInt("has_hard") != 0) {
      return firstHard;
    }
    if (row.getInt("has_good") != 0) {
      return firstGood;
    }
    if (row.getInt("has_easy") != 0) {
      return firstEasy;
    }
    throw new IllegalStateException("still-New mapped first-rating row had no mapped grade");
  }
}
