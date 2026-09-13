package com.odde.donut.services.notebookGit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One-time, raw-JDBC upgrade recipe that migrates every retained legacy soft-deleted note into
 * location-based web trash (a {@code _trash} folder subtree inside the note's own notebook),
 * clearing the legacy {@code note.deleted_at} marker only after valid placement.
 *
 * <p>Mirrors {@link com.odde.donut.services.FolderConstructionService#ensureTrashParentFor} and
 * {@link com.odde.donut.services.NoteMotionService#executeMoveIntoFolderWithAvailableTitle} without
 * any JPA/Spring dependency, so it can run from a Flyway {@code BaseJavaMigration} before the
 * application's {@code EntityManagerFactory} is available.
 *
 * <p>Migration-owned and JDBC-capable. Kept outside the auto-discovered versioned Flyway directory
 * so its cases can be validated before it is registered immutably.
 */
public final class NoteLegacyTrashMigration {

  static final String TRASH_ROOT_NAME = "_trash";

  private static final String CANDIDATE_NOTES_QUERY =
      """
      SELECT id, notebook_id, folder_id, title
      FROM note
      WHERE deleted_at IS NOT NULL AND notebook_id IS NOT NULL
      ORDER BY notebook_id ASC, id ASC
      """;

  private static final String FIND_TRASH_ROOT =
      "SELECT id FROM folder WHERE notebook_id = ? AND parent_folder_id IS NULL AND LOWER(name) = ?";

  private static final String INSERT_FOLDER =
      """
      INSERT INTO folder (notebook_id, parent_folder_id, name, created_at, updated_at)
      VALUES (?, ?, ?, ?, ?)
      """;

  private static final String FIND_CHILD_FOLDER =
      "SELECT id FROM folder WHERE notebook_id = ? AND parent_folder_id = ? AND name = ?";

  private static final String FIND_FOLDER_BY_ID =
      "SELECT id, parent_folder_id, name FROM folder WHERE id = ?";

  private static final String FIND_CONFLICTING_NOTE =
      """
      SELECT id FROM note
      WHERE notebook_id = ? AND folder_id = ? AND LOWER(title) = LOWER(?) AND id <> ?
      LIMIT 1
      """;

  private static final String UPDATE_NOTE =
      "UPDATE note SET folder_id = ?, title = ?, deleted_at = NULL, updated_at = ? WHERE id = ?";

  private NoteLegacyTrashMigration() {}

  /**
   * Migrates every legacy soft-deleted note into its notebook's {@code _trash} subtree, clearing
   * {@code deleted_at}. Commits the whole migration atomically and rolls back on error so no
   * partial placement is left behind. Restores the connection's original auto-commit mode when done
   * (or on failure). Idempotent: migrated notes have {@code deleted_at = NULL} and are no longer
   * candidates, so a retry after a clean failure migrates only the remaining notes.
   */
  public static void run(Connection connection, Instant migrationTime) throws SQLException {
    boolean originalAutoCommit = connection.getAutoCommit();
    connection.setAutoCommit(false);
    try {
      Timestamp now = Timestamp.from(migrationTime);
      for (NoteRow note : candidateNotes(connection)) {
        migrateNote(connection, note, now);
      }
      connection.commit();
    } catch (SQLException | RuntimeException e) {
      connection.rollback();
      throw e;
    } finally {
      connection.setAutoCommit(originalAutoCommit);
    }
  }

  static List<NoteRow> candidateNotes(Connection connection) throws SQLException {
    List<NoteRow> notes = new ArrayList<>();
    try (PreparedStatement statement = connection.prepareStatement(CANDIDATE_NOTES_QUERY);
        ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        notes.add(
            new NoteRow(
                resultSet.getInt("id"),
                resultSet.getInt("notebook_id"),
                NotebookGitRows.readNullableInt(resultSet, "folder_id"),
                resultSet.getString("title")));
      }
    }
    return notes;
  }

  private static void migrateNote(Connection connection, NoteRow note, Timestamp now)
      throws SQLException {
    int notebookId = note.notebookId();
    List<FolderRow> trail =
        note.folderId() == null
            ? List.of()
            : folderTrailFromRootToContaining(connection, note.folderId());
    Integer destinationFolderId;
    if (!trail.isEmpty() && trail.get(0).name().equalsIgnoreCase(TRASH_ROOT_NAME)) {
      destinationFolderId = note.folderId();
    } else {
      Integer trashRootId = findOrCreateTrashRoot(connection, notebookId, now);
      destinationFolderId = trashRootId;
      for (FolderRow segment : trail) {
        destinationFolderId =
            findOrCreateChildFolder(
                connection, notebookId, destinationFolderId, segment.name(), now);
      }
    }
    String availableTitle =
        availableTitleAt(connection, notebookId, destinationFolderId, note.title(), note.id());
    try (PreparedStatement statement = connection.prepareStatement(UPDATE_NOTE)) {
      statement.setInt(1, destinationFolderId);
      statement.setString(2, availableTitle);
      statement.setTimestamp(3, now);
      statement.setInt(4, note.id());
      statement.executeUpdate();
    }
  }

  private static Integer findOrCreateTrashRoot(Connection connection, int notebookId, Timestamp now)
      throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(FIND_TRASH_ROOT)) {
      statement.setInt(1, notebookId);
      statement.setString(2, TRASH_ROOT_NAME.toLowerCase());
      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return resultSet.getInt("id");
        }
      }
    }
    return insertFolder(connection, notebookId, null, TRASH_ROOT_NAME, now);
  }

  private static Integer findOrCreateChildFolder(
      Connection connection, int notebookId, Integer parentFolderId, String name, Timestamp now)
      throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(FIND_CHILD_FOLDER)) {
      statement.setInt(1, notebookId);
      statement.setInt(2, parentFolderId);
      statement.setString(3, name);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return resultSet.getInt("id");
        }
      }
    }
    return insertFolder(connection, notebookId, parentFolderId, name, now);
  }

  private static Integer insertFolder(
      Connection connection, int notebookId, Integer parentFolderId, String name, Timestamp now)
      throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement(INSERT_FOLDER, Statement.RETURN_GENERATED_KEYS)) {
      statement.setInt(1, notebookId);
      if (parentFolderId == null) {
        statement.setNull(2, Types.INTEGER);
      } else {
        statement.setInt(2, parentFolderId);
      }
      statement.setString(3, name);
      statement.setTimestamp(4, now);
      statement.setTimestamp(5, now);
      statement.executeUpdate();
      try (ResultSet keys = statement.getGeneratedKeys()) {
        keys.next();
        return keys.getInt(1);
      }
    }
  }

  private static List<FolderRow> folderTrailFromRootToContaining(
      Connection connection, Integer folderId) throws SQLException {
    List<FolderRow> chain = new ArrayList<>();
    Integer currentId = folderId;
    while (currentId != null) {
      try (PreparedStatement statement = connection.prepareStatement(FIND_FOLDER_BY_ID)) {
        statement.setInt(1, currentId);
        try (ResultSet resultSet = statement.executeQuery()) {
          if (!resultSet.next()) {
            break;
          }
          chain.add(
              new FolderRow(
                  resultSet.getInt("id"),
                  NotebookGitRows.readNullableInt(resultSet, "parent_folder_id"),
                  resultSet.getString("name")));
          currentId = NotebookGitRows.readNullableInt(resultSet, "parent_folder_id");
        }
      }
    }
    Collections.reverse(chain);
    return chain;
  }

  private static String availableTitleAt(
      Connection connection,
      int notebookId,
      Integer folderId,
      String requestedTitle,
      int sourceNoteId)
      throws SQLException {
    if (!isTitleOccupied(connection, notebookId, folderId, requestedTitle, sourceNoteId)) {
      return requestedTitle;
    }
    for (int suffixNumber = 2; ; suffixNumber++) {
      String suffix = " (" + suffixNumber + ")";
      String candidate =
          requestedTitle.substring(
                  0,
                  Math.min(
                      requestedTitle.length(),
                      com.odde.donut.entities.Note.MAX_TITLE_LENGTH - suffix.length()))
              + suffix;
      if (!isTitleOccupied(connection, notebookId, folderId, candidate, sourceNoteId)) {
        return candidate;
      }
    }
  }

  private static boolean isTitleOccupied(
      Connection connection, int notebookId, Integer folderId, String title, int sourceNoteId)
      throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(FIND_CONFLICTING_NOTE)) {
      statement.setInt(1, notebookId);
      statement.setInt(2, folderId);
      statement.setString(3, title);
      statement.setInt(4, sourceNoteId);
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next();
      }
    }
  }

  record NoteRow(int id, int notebookId, Integer folderId, String title) {}

  record FolderRow(int id, Integer parentFolderId, String name) {}
}
