package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.WikiLinkMarkdownRewrite;
import com.odde.doughnut.validators.DisplayNamePathSeparators;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One-shot JDBC backfill: convert OS-invalid characters in note/folder/notebook display names,
 * rewrite inbound wiki / path-Markdown tokens, and rewrite wiki-title cache {@code link_text} to
 * the converted spellings.
 */
public final class DisplayNameOsInvalidCharsBackfill {

  private DisplayNameOsInvalidCharsBackfill() {}

  public static void run(Connection connection) throws SQLException {
    DisplayNameUniqueKeyJdbcConversion.convertNoteFolderAndNotebookNames(
        connection,
        DisplayNamePathSeparators::normalizeDisplayName,
        DisplayNameOsInvalidCharsBackfill::blank,
        DisplayNameOsInvalidCharsBackfill::conflict);
    rewriteMarkdownColumn(
        connection, "SELECT id, content FROM note", "UPDATE note SET content = ? WHERE id = ?");
    rewriteMarkdownColumn(
        connection,
        "SELECT id, readme_content FROM folder",
        "UPDATE folder SET readme_content = ? WHERE id = ?");
    rewriteMarkdownColumn(
        connection,
        "SELECT id, readme_content FROM notebook",
        "UPDATE notebook SET readme_content = ? WHERE id = ?");
    rewriteWikiTitleCache(connection);
  }

  private static void rewriteMarkdownColumn(
      Connection connection, String selectSql, String updateSql) throws SQLException {
    List<MarkdownRow> rows = new ArrayList<>();
    try (PreparedStatement select = connection.prepareStatement(selectSql);
        ResultSet rs = select.executeQuery()) {
      while (rs.next()) {
        rows.add(new MarkdownRow(rs.getInt("id"), rs.getString(2)));
      }
    }
    try (PreparedStatement update = connection.prepareStatement(updateSql)) {
      for (MarkdownRow row : rows) {
        if (row.markdown() == null || row.markdown().isEmpty()) {
          continue;
        }
        String rewritten =
            WikiLinkMarkdownRewrite.replaceOsInvalidCharsInAuthoredTokens(row.markdown());
        if (rewritten.equals(row.markdown())) {
          continue;
        }
        update.setString(1, rewritten);
        update.setInt(2, row.id());
        update.executeUpdate();
      }
    }
  }

  private static void rewriteWikiTitleCache(Connection connection) throws SQLException {
    record CacheRow(int id, int noteId, String linkText) {}
    List<CacheRow> rows = new ArrayList<>();
    try (PreparedStatement select =
            connection.prepareStatement(
                "SELECT id, note_id, link_text FROM note_wiki_title_cache");
        ResultSet rs = select.executeQuery()) {
      while (rs.next()) {
        rows.add(new CacheRow(rs.getInt("id"), rs.getInt("note_id"), rs.getString("link_text")));
      }
    }
    Map<Integer, Set<String>> textsByNote = new HashMap<>();
    for (CacheRow row : rows) {
      textsByNote.computeIfAbsent(row.noteId(), _ -> new HashSet<>()).add(row.linkText());
    }
    try (PreparedStatement update =
            connection.prepareStatement(
                "UPDATE note_wiki_title_cache SET link_text = ? WHERE id = ?");
        PreparedStatement delete =
            connection.prepareStatement("DELETE FROM note_wiki_title_cache WHERE id = ?")) {
      for (CacheRow row : rows) {
        String converted =
            WikiLinkMarkdownRewrite.replaceOsInvalidCharsInStoredLinkInner(row.linkText());
        if (converted.equals(row.linkText())) {
          continue;
        }
        Set<String> texts = textsByNote.get(row.noteId());
        texts.remove(row.linkText());
        if (texts.contains(converted)) {
          delete.setInt(1, row.id());
          delete.executeUpdate();
          continue;
        }
        update.setString(1, converted);
        update.setInt(2, row.id());
        update.executeUpdate();
        texts.add(converted);
      }
    }
  }

  private static IllegalStateException blank(
      String table, int id, String rawValue, String uniqueKey) {
    return new IllegalStateException(
        "Display-name OS-invalid backfill aborted: converting would leave a blank name on "
            + table
            + " id="
            + id
            + " raw=["
            + rawValue
            + "] unique key "
            + uniqueKey);
  }

  private static IllegalStateException conflict(
      String table,
      int updatingId,
      int conflictingId,
      String rawValue,
      String conflictingRawValue,
      String normalized,
      String uniqueKey) {
    return new IllegalStateException(
        "Display-name OS-invalid backfill aborted: converting would violate "
            + uniqueKey
            + " on "
            + table
            + " id="
            + updatingId
            + " raw=["
            + rawValue
            + "] normalized=["
            + normalized
            + "] conflicts with id="
            + conflictingId
            + " raw=["
            + conflictingRawValue
            + "]");
  }

  private record MarkdownRow(int id, String markdown) {}
}
