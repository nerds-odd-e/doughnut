package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.NoteContentMarkdown;
import com.odde.doughnut.algorithms.WikiLinkMarkdown;
import com.odde.doughnut.algorithms.WikiLinkTargetReference;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

/**
 * JDBC backfill for {@code note_property_index.target_note_id} via note title and notebook name.
 */
public final class NotePropertyIndexTargetNoteResolutionBackfill {

  private static final String ROWS_QUERY =
      """
      SELECT i.id, i.property_key, n.content, nb.name AS notebook_name
      FROM note_property_index i
      INNER JOIN note n ON n.id = i.note_id
      INNER JOIN notebook nb ON nb.id = n.notebook_id
      WHERE n.deleted_at IS NULL
      """;

  private static final String TARGET_LOOKUP =
      """
      SELECT t.id
      FROM note t
      INNER JOIN notebook nb ON nb.id = t.notebook_id
      WHERE LOWER(t.title) = LOWER(?)
        AND t.deleted_at IS NULL
        AND nb.deleted_at IS NULL
        AND LOWER(nb.name) = LOWER(?)
      ORDER BY t.id ASC
      LIMIT 1
      """;

  private static final String UPDATE_TARGET =
      "UPDATE note_property_index SET target_note_id = ? WHERE id = ?";

  private NotePropertyIndexTargetNoteResolutionBackfill() {}

  public static void run(Connection connection) throws SQLException {
    try (PreparedStatement rowsStmt = connection.prepareStatement(ROWS_QUERY);
        PreparedStatement lookupStmt = connection.prepareStatement(TARGET_LOOKUP);
        PreparedStatement updateStmt = connection.prepareStatement(UPDATE_TARGET);
        ResultSet rows = rowsStmt.executeQuery()) {
      while (rows.next()) {
        int indexId = rows.getInt("id");
        String propertyKey = rows.getString("property_key");
        String content = rows.getString("content");
        String notebookName = rows.getString("notebook_name");

        Optional<Integer> targetNoteId =
            resolveTargetNoteIdFromProperty(content, propertyKey, notebookName, lookupStmt);
        if (targetNoteId.isPresent()) {
          updateStmt.setInt(1, targetNoteId.get());
        } else {
          updateStmt.setNull(1, Types.INTEGER);
        }
        updateStmt.setInt(2, indexId);
        updateStmt.addBatch();
      }
      updateStmt.executeBatch();
    }
  }

  private static Optional<Integer> resolveTargetNoteIdFromProperty(
      String content, String propertyKey, String focusNotebookName, PreparedStatement lookupStmt)
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
    Optional<WikiLinkTargetReference> reference =
        WikiLinkTargetReference.forToken(linkTokens.getFirst(), focusNotebookName);
    if (reference.isEmpty()) {
      return Optional.empty();
    }
    WikiLinkTargetReference ref = reference.get();
    return lookupTargetNoteId(ref.notebookName(), ref.noteTitle(), lookupStmt);
  }

  private static Optional<Integer> lookupTargetNoteId(
      String notebookName, String noteTitle, PreparedStatement lookupStmt) throws SQLException {
    lookupStmt.setString(1, noteTitle);
    lookupStmt.setString(2, notebookName);
    try (ResultSet rs = lookupStmt.executeQuery()) {
      if (!rs.next()) {
        return Optional.empty();
      }
      return Optional.of(rs.getInt("id"));
    }
  }
}
