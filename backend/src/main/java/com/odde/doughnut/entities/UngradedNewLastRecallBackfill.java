package com.odde.doughnut.entities;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class UngradedNewLastRecallBackfill {

  private UngradedNewLastRecallBackfill() {}

  public static void run(Connection connection) throws SQLException {
    try (PreparedStatement update =
        connection.prepareStatement(
            """
            UPDATE memory_tracker
            SET last_recalled_at = NULL
            WHERE stability = 0
              AND difficulty IS NULL
              AND last_recalled_at IS NOT NULL
              AND NOT EXISTS (
                SELECT 1 FROM recall_log
                WHERE recall_log.memory_tracker_id = memory_tracker.id
                  AND recall_log.product_outcome IN (
                    'GOOD', 'EASY', 'HARD', 'SHRINK', 'AGAIN', 'AGAIN_ZERO'
                  )
              )
            """)) {
      update.executeUpdate();
    }
  }
}
