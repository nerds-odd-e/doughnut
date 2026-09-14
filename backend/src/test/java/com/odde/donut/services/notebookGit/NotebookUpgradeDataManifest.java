package com.odde.donut.services.notebookGit;

import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

record NotebookUpgradeDataManifest(
    List<NoteRow> notes,
    List<MemoryTrackerRow> memoryTrackers,
    List<FolderRow> folders,
    List<AuthoredReferenceRow> authoredReferences) {

  static NotebookUpgradeDataManifest capture(JdbcTemplate jdbcTemplate, int notebookId) {
    return new NotebookUpgradeDataManifest(
        captureNotes(jdbcTemplate, notebookId),
        captureMemoryTrackers(jdbcTemplate, notebookId),
        captureFolders(jdbcTemplate, notebookId),
        captureAuthoredReferences(jdbcTemplate, notebookId));
  }

  private static List<NoteRow> captureNotes(JdbcTemplate jdbcTemplate, int notebookId) {
    return jdbcTemplate.query(
        """
        SELECT id, title, content, notebook_id, folder_id, created_at, updated_at
        FROM note
        WHERE notebook_id = ?
        ORDER BY id ASC
        """,
        (rs, ignored) ->
            new NoteRow(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("content"),
                rs.getInt("notebook_id"),
                rs.wasNull() ? null : rs.getInt("folder_id"),
                rs.getTimestamp("created_at"),
                rs.getTimestamp("updated_at")),
        notebookId);
  }

  private static List<MemoryTrackerRow> captureMemoryTrackers(
      JdbcTemplate jdbcTemplate, int notebookId) {
    return jdbcTemplate.query(
        """
        SELECT mt.id, mt.note_id, mt.user_id, mt.type, mt.property_key,
               mt.stability, mt.difficulty, mt.removed_from_tracking
        FROM memory_tracker mt
        JOIN note n ON mt.note_id = n.id
        WHERE n.notebook_id = ?
        ORDER BY mt.id ASC
        """,
        (rs, ignored) ->
            new MemoryTrackerRow(
                rs.getInt("id"),
                rs.getInt("note_id"),
                rs.getInt("user_id"),
                rs.getString("type"),
                rs.getString("property_key"),
                rs.getFloat("stability"),
                rs.getFloat("difficulty"),
                rs.getBoolean("removed_from_tracking")),
        notebookId);
  }

  private static List<FolderRow> captureFolders(JdbcTemplate jdbcTemplate, int notebookId) {
    return jdbcTemplate.query(
        """
        SELECT id, notebook_id, parent_folder_id, name, readme_content
        FROM folder
        WHERE notebook_id = ?
        ORDER BY id ASC
        """,
        (rs, ignored) ->
            new FolderRow(
                rs.getInt("id"),
                rs.getInt("notebook_id"),
                rs.wasNull() ? null : rs.getInt("parent_folder_id"),
                rs.getString("name"),
                rs.getString("readme_content")),
        notebookId);
  }

  private static List<AuthoredReferenceRow> captureAuthoredReferences(
      JdbcTemplate jdbcTemplate, int notebookId) {
    return jdbcTemplate.query(
        """
        SELECT r.id, r.source_note_id, r.kind, r.authored_link, r.display_text,
               r.document_order, r.note_id_url_note_id
        FROM authored_note_reference r
        JOIN note n ON r.source_note_id = n.id
        WHERE n.notebook_id = ?
        ORDER BY r.id ASC
        """,
        (rs, ignored) ->
            new AuthoredReferenceRow(
                rs.getInt("id"),
                rs.getInt("source_note_id"),
                rs.getString("kind"),
                rs.getString("authored_link"),
                rs.getString("display_text"),
                rs.getInt("document_order"),
                rs.wasNull() ? null : rs.getInt("note_id_url_note_id")),
        notebookId);
  }

  private record NoteRow(
      int id,
      String title,
      String content,
      int notebookId,
      Integer folderId,
      Timestamp createdAt,
      Timestamp updatedAt) {}

  private record MemoryTrackerRow(
      int id,
      int noteId,
      int userId,
      String type,
      String propertyKey,
      float stability,
      float difficulty,
      boolean removedFromTracking) {}

  private record FolderRow(
      int id, int notebookId, Integer parentFolderId, String name, String readmeContent) {}

  private record AuthoredReferenceRow(
      int id,
      int sourceNoteId,
      String kind,
      String authoredLink,
      String displayText,
      int documentOrder,
      Integer noteIdUrlNoteId) {}
}
