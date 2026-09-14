package com.odde.donut.services.notebookGit;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

/**
 * Shared raw-JDBC row builders for seeding a pre-326 {@link PreUpgradeFixtureSchema} fixture:
 * user/notebook/folder/note/binding inserts shaped for the legacy-trash conversion and baseline
 * rebuild proofs (own-user notebooks, nested folders with optional readme content, notes with an
 * optional legacy {@code deleted_at}). Used directly against JDBC because the fixture schema
 * predates the entities' current Flyway-migrated shape, so JPA/`makeMe` cannot construct these
 * rows.
 */
final class PreUpgradeFixtureRows {

  private PreUpgradeFixtureRows() {}

  static Timestamp legacyDeletedAt() {
    return Timestamp.valueOf("2019-01-01 00:00:00");
  }

  static Timestamp seedTimestamp() {
    return Timestamp.valueOf("2020-06-01 00:00:00");
  }

  static int insertUser(Connection connection, String name, String externalIdentifier)
      throws SQLException {
    return insertReturningId(
        connection,
        "INSERT INTO user (name, external_identifier) VALUES ('"
            + name
            + "', '"
            + externalIdentifier
            + "')");
  }

  static int insertNotebook(Connection connection, int ownerId, String name, boolean deleted)
      throws SQLException {
    int ownershipId =
        insertReturningId(connection, "INSERT INTO ownership (user_id) VALUES (" + ownerId + ")");
    String deletedAtValue = deleted ? "'" + legacyDeletedAt() + "'" : "NULL";
    return insertReturningId(
        connection,
        "INSERT INTO notebook (ownership_id, creator_id, name, deleted_at, created_at, updated_at)"
            + " VALUES ("
            + ownershipId
            + ", "
            + ownerId
            + ", '"
            + name
            + "', "
            + deletedAtValue
            + ", '"
            + seedTimestamp()
            + "', '"
            + seedTimestamp()
            + "')");
  }

  static int insertFolder(
      Connection connection,
      int notebookId,
      Integer parentFolderId,
      String name,
      String readmeContent)
      throws SQLException {
    return insertReturningId(
        connection,
        "INSERT INTO folder (notebook_id, parent_folder_id, name, readme_content, created_at,"
            + " updated_at) VALUES ("
            + notebookId
            + ", "
            + (parentFolderId == null ? "NULL" : parentFolderId)
            + ", '"
            + name
            + "', "
            + (readmeContent == null ? "NULL" : "'" + readmeContent + "'")
            + ", '"
            + seedTimestamp()
            + "', '"
            + seedTimestamp()
            + "')");
  }

  static int insertNote(
      Connection connection,
      int notebookId,
      Integer folderId,
      String title,
      String content,
      Timestamp deletedAt)
      throws SQLException {
    return insertReturningId(
        connection,
        "INSERT INTO note (notebook_id, folder_id, title, content, deleted_at, created_at,"
            + " updated_at) VALUES ("
            + notebookId
            + ", "
            + (folderId == null ? "NULL" : folderId)
            + ", '"
            + title
            + "', '"
            + content
            + "', "
            + (deletedAt == null ? "NULL" : "'" + deletedAt + "'")
            + ", '"
            + seedTimestamp()
            + "', '"
            + seedTimestamp()
            + "')");
  }

  static void insertBinding(Connection connection, int notebookId, String gitObjectId)
      throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute(
          "INSERT INTO notebook_git_binding (notebook_id, accepted_git_object_id, bundle_bytes,"
              + " created_at, updated_at) VALUES ("
              + notebookId
              + ", '"
              + gitObjectId
              + "', X'0102030405', '2024-01-01 00:00:00', '2024-01-01 00:00:00')");
    }
  }

  static int insertReturningId(Connection connection, String sql) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute(sql, Statement.RETURN_GENERATED_KEYS);
      try (ResultSet keys = statement.getGeneratedKeys()) {
        keys.next();
        return keys.getInt(1);
      }
    }
  }

  static Integer nullableInt(ResultSet rs, String column) throws SQLException {
    int value = rs.getInt(column);
    return rs.wasNull() ? null : value;
  }
}
