package com.odde.donut.services;

import com.odde.donut.algorithms.FrontmatterNoteLevel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

/**
 * One-time JDBC backfill of frontmatter {@code note_level} and its cache from {@code note.level}.
 */
public final class NoteLevelIndexBackfill {

  private static final String NOTES_QUERY =
      """
      SELECT id, content, level
      FROM note
      WHERE deleted_at IS NULL
      """;

  private static final String UPDATE_CONTENT = "UPDATE note SET content = ? WHERE id = ?";

  private static final String UPSERT_INDEX =
      """
      INSERT INTO note_level_index (note_id, level) VALUES (?, ?) AS incoming
      ON DUPLICATE KEY UPDATE level = incoming.level
      """;

  private NoteLevelIndexBackfill() {}

  public static void run(Connection connection) throws SQLException {
    try (PreparedStatement notesStmt = connection.prepareStatement(NOTES_QUERY);
        PreparedStatement updateContentStmt = connection.prepareStatement(UPDATE_CONTENT);
        PreparedStatement upsertIndexStmt = connection.prepareStatement(UPSERT_INDEX)) {
      try (ResultSet notes = notesStmt.executeQuery()) {
        while (notes.next()) {
          int noteId = notes.getInt("id");
          String content = notes.getString("content");
          int legacyLevel = notes.getInt("level");

          String updated = FrontmatterNoteLevel.withVerbatimLevel(content, legacyLevel);
          if (!Objects.equals(updated, content)) {
            updateContentStmt.setString(1, updated);
            updateContentStmt.setInt(2, noteId);
            updateContentStmt.addBatch();
          }

          Optional<Integer> level = FrontmatterNoteLevel.fromNoteContent(updated);
          if (level.isPresent()) {
            upsertIndexStmt.setInt(1, noteId);
            upsertIndexStmt.setInt(2, level.get());
            upsertIndexStmt.addBatch();
          }
        }
      }
      updateContentStmt.executeBatch();
      upsertIndexStmt.executeBatch();
    }
  }
}
