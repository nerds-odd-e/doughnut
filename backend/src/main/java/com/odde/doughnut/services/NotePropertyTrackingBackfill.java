package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.NoteContentMarkdown;
import com.odde.doughnut.algorithms.NotePropertyIndexPlanner;
import com.odde.doughnut.algorithms.PropertyTrackingBackfillPlan;
import com.odde.doughnut.entities.ForgettingCurve;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** One-time JDBC backfill for note property index rows and skipped property trackers. */
public final class NotePropertyTrackingBackfill {

  private static final String NOTES_QUERY =
      """
      SELECT n.id, n.content, o.user_id
      FROM note n
      INNER JOIN notebook nb ON n.notebook_id = nb.id
      INNER JOIN ownership o ON nb.ownership_id = o.id
      WHERE n.deleted_at IS NULL
      """;

  private static final String EXISTING_PROPERTY_KEYS_QUERY =
      """
      SELECT property_key
      FROM memory_tracker
      WHERE user_id = ?
        AND note_id = ?
        AND deleted_at IS NULL
        AND spelling = 0
        AND property_key <> ''
      """;

  private static final String INSERT_INDEX =
      "INSERT IGNORE INTO note_property_index (note_id, property_key, item_index) VALUES (?, ?, ?)";

  private static final String TRACKER_EXISTS_QUERY =
      """
      SELECT 1
      FROM memory_tracker
      WHERE user_id = ?
        AND note_id = ?
        AND spelling = 0
        AND property_key = ?
        AND deleted_at IS NULL
      LIMIT 1
      """;

  private static final String INSERT_SKIPPED_TRACKER =
      """
      INSERT INTO memory_tracker (
        user_id,
        note_id,
        spelling,
        property_key,
        removed_from_tracking,
        assimilated_at,
        last_recalled_at,
        next_recall_at,
        forgetting_curve_index,
        recall_count
      ) VALUES (?, ?, 0, ?, 1, ?, ?, ?, ?, 0)
      """;

  private NotePropertyTrackingBackfill() {}

  public static void run(Connection connection, Timestamp now) throws SQLException {
    try (PreparedStatement notesStmt = connection.prepareStatement(NOTES_QUERY);
        PreparedStatement insertIndexStmt = connection.prepareStatement(INSERT_INDEX);
        PreparedStatement existingKeysStmt =
            connection.prepareStatement(EXISTING_PROPERTY_KEYS_QUERY);
        PreparedStatement trackerExistsStmt = connection.prepareStatement(TRACKER_EXISTS_QUERY);
        PreparedStatement insertTrackerStmt = connection.prepareStatement(INSERT_SKIPPED_TRACKER)) {

      insertTrackerStmt.setFloat(7, ForgettingCurve.DEFAULT_FORGETTING_CURVE_INDEX);

      try (ResultSet notes = notesStmt.executeQuery()) {
        while (notes.next()) {
          int noteId = notes.getInt("id");
          String content = notes.getString("content");
          Integer ownerUserId = readNullableInt(notes, "user_id");

          var frontmatter =
              NoteContentMarkdown.splitLeadingFrontmatter(content == null ? "" : content)
                  .map(NoteContentMarkdown.LeadingFrontmatter::frontmatter)
                  .filter(fm -> !fm.isEmpty())
                  .orElse(null);
          if (frontmatter == null) {
            continue;
          }

          Set<String> existingPropertyKeys =
              ownerUserId == null
                  ? Set.of()
                  : loadExistingPropertyKeys(existingKeysStmt, ownerUserId, noteId);

          List<NotePropertyIndexPlanner.PlannedRow> plannedRows =
              NotePropertyIndexPlanner.plannedRows(frontmatter);
          PropertyTrackingBackfillPlan.Result plan =
              PropertyTrackingBackfillPlan.forPlannedRows(plannedRows, existingPropertyKeys);

          for (NotePropertyIndexPlanner.PlannedRow row : plannedRows) {
            insertIndexStmt.setInt(1, noteId);
            insertIndexStmt.setString(2, row.propertyKey());
            insertIndexStmt.setInt(3, row.itemIndex());
            insertIndexStmt.addBatch();
          }

          if (ownerUserId != null) {
            for (String key : plan.keysToSeedSkipped()) {
              if (!trackerExists(trackerExistsStmt, ownerUserId, noteId, key)) {
                insertTrackerStmt.setInt(1, ownerUserId);
                insertTrackerStmt.setInt(2, noteId);
                insertTrackerStmt.setString(3, key);
                insertTrackerStmt.setTimestamp(4, now);
                insertTrackerStmt.setTimestamp(5, now);
                insertTrackerStmt.setTimestamp(6, now);
                insertTrackerStmt.addBatch();
              }
            }
          }
        }
      }

      insertIndexStmt.executeBatch();
      insertTrackerStmt.executeBatch();
    }
  }

  private static Integer readNullableInt(ResultSet rs, String column) throws SQLException {
    Object value = rs.getObject(column);
    if (value == null) {
      return null;
    }
    return ((Number) value).intValue();
  }

  private static Set<String> loadExistingPropertyKeys(
      PreparedStatement stmt, int userId, int noteId) throws SQLException {
    stmt.setInt(1, userId);
    stmt.setInt(2, noteId);
    Set<String> keys = new HashSet<>();
    try (ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        keys.add(rs.getString("property_key"));
      }
    }
    return keys;
  }

  private static boolean trackerExists(
      PreparedStatement stmt, int userId, int noteId, String propertyKey) throws SQLException {
    stmt.setInt(1, userId);
    stmt.setInt(2, noteId);
    stmt.setString(3, propertyKey);
    try (ResultSet rs = stmt.executeQuery()) {
      return rs.next();
    }
  }
}
