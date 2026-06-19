package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.NoteContentMarkdown;
import com.odde.doughnut.algorithms.WikiLinkMarkdown;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** One-time JDBC backfill for {@code note_property_index.target_note_id} from wiki title cache. */
public final class NotePropertyIndexTargetNoteBackfill {

  private static final String ROWS_QUERY =
      """
      SELECT i.id, i.note_id, i.property_key, n.content
      FROM note_property_index i
      INNER JOIN note n ON n.id = i.note_id
      WHERE n.deleted_at IS NULL
      """;

  private static final String TARGET_LOOKUP =
      """
      SELECT target_note_id
      FROM note_wiki_title_cache
      WHERE note_id = ?
        AND link_text = ?
      LIMIT 1
      """;

  private static final String UPDATE_TARGET =
      "UPDATE note_property_index SET target_note_id = ? WHERE id = ?";

  private NotePropertyIndexTargetNoteBackfill() {}

  public static void run(Connection connection) throws SQLException {
    try (PreparedStatement rowsStmt = connection.prepareStatement(ROWS_QUERY);
        PreparedStatement lookupStmt = connection.prepareStatement(TARGET_LOOKUP);
        PreparedStatement updateStmt = connection.prepareStatement(UPDATE_TARGET);
        ResultSet rows = rowsStmt.executeQuery()) {
      while (rows.next()) {
        int indexId = rows.getInt("id");
        int noteId = rows.getInt("note_id");
        String propertyKey = rows.getString("property_key");
        String content = rows.getString("content");

        Optional<Integer> targetNoteId =
            resolveTargetNoteIdFromProperty(content, propertyKey, noteId, lookupStmt);
        if (targetNoteId.isEmpty()) {
          continue;
        }
        updateStmt.setInt(1, targetNoteId.get());
        updateStmt.setInt(2, indexId);
        updateStmt.addBatch();
      }
      updateStmt.executeBatch();
    }
  }

  private static Optional<Integer> resolveTargetNoteIdFromProperty(
      String content, String propertyKey, int noteId, PreparedStatement lookupStmt)
      throws SQLException {
    Optional<String> propertyValue =
        NoteContentMarkdown.splitLeadingFrontmatter(content == null ? "" : content)
            .flatMap(lf -> lf.frontmatter().getString(propertyKey));
    if (propertyValue.isEmpty()) {
      return Optional.empty();
    }
    List<String> linkTokens = WikiLinkMarkdown.innerTitlesInOccurrenceOrder(propertyValue.get());
    if (linkTokens.isEmpty()) {
      return Optional.empty();
    }
    return lookupTargetNoteId(noteId, linkTokens.getFirst(), lookupStmt);
  }

  private static Optional<Integer> lookupTargetNoteId(
      int noteId, String linkText, PreparedStatement lookupStmt) throws SQLException {
    lookupStmt.setInt(1, noteId);
    lookupStmt.setString(2, linkText);
    try (ResultSet rs = lookupStmt.executeQuery()) {
      if (!rs.next()) {
        return Optional.empty();
      }
      return Optional.of(rs.getInt("target_note_id"));
    }
  }
}
