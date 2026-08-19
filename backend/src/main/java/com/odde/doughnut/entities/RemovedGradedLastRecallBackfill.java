package com.odde.doughnut.entities;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class RemovedGradedLastRecallBackfill {

  private RemovedGradedLastRecallBackfill() {}

  public static void run(Connection connection) throws SQLException {
    try (PreparedStatement update =
        connection.prepareStatement(
            """
            UPDATE memory_tracker
            INNER JOIN (
              SELECT memory_tracker_id, MAX(recorded_at) AS last_mapped_at
              FROM recall_log
              WHERE product_outcome IN (
                %s, 'SHRINK', 'AGAIN_ZERO'
              )
              GROUP BY memory_tracker_id
            ) mapped ON mapped.memory_tracker_id = memory_tracker.id
            SET memory_tracker.last_recalled_at = mapped.last_mapped_at
            WHERE memory_tracker.removed_from_tracking IS TRUE
            """
                .formatted(ProductOutcome.mappedGradeSqlInList()))) {
      update.executeUpdate();
    }
  }
}
