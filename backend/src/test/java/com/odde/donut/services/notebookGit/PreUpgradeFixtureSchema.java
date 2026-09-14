package com.odde.donut.services.notebookGit;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.configuration.FluentConfiguration;

/**
 * Owns a disposable MySQL schema for rehearsing the actual registered Flyway migration chain
 * (V300000326-V300000328, see {@code db.migration.V300000326__MigrateLegacyDeletedNotesToTrash}
 * through {@code V300000328__drop_note_deleted_at.sql}) against real, durable DDL ahead of any
 * upgrade boundary, instead of copying migration SQL into a temporary table (the technique used by
 * {@link com.odde.donut.services.MemoryTrackerDeletedAtUpgradeTest}, deliberately not reused here).
 *
 * <p>{@link #createAtVersion325} migrates a fresh schema with the project's actual migration
 * resources up to and including V300000325 — the last version before the legacy-trash conversion
 * begins — then leaves an open raw JDBC connection into it so callers can seed pre-upgrade fixture
 * rows (including the still-present {@code note.deleted_at} column) before continuing the actual
 * migration chain from V300000326 onward in a later test.
 *
 * <p>Creates, grants and drops the schema using the same passwordless local root MySQL
 * administration and per-schema grant that {@code scripts/backend-test-worktree-owner.sh} and the
 * worktree retirement tooling already rely on (see {@code docs/worktree-backend-tests.md}); the
 * actual migration and fixture connection then authenticates as the ordinary {@code doughnut} test
 * user, exactly as the real application does. The schema name is derived from the enclosing test
 * suite's own live schema, so it always sits alongside whichever database that suite is actually
 * using, and {@link #close} drops only this owned schema — the enclosing suite schema is never read
 * from, written to, or dropped by this class.
 */
final class PreUpgradeFixtureSchema implements AutoCloseable {

  private static final String ADMIN_HOST_AND_PORT = "127.0.0.1:3309";
  private static final String ADMIN_URL =
      "jdbc:mysql://" + ADMIN_HOST_AND_PORT + "/?connectionTimeZone=UTC";
  private static final String ADMIN_USER = "root";
  private static final String ADMIN_PASSWORD = "";
  private static final String APP_USER = "doughnut";
  private static final String APP_PASSWORD = "doughnut";
  private static final String PRE_UPGRADE_BOUNDARY_VERSION = "300000325";
  private static final String SCHEMA_SUFFIX = "_v325_fixture";

  private final String schemaName;
  private final String schemaUrl;
  private final Connection connection;

  private PreUpgradeFixtureSchema(String schemaName, String schemaUrl, Connection connection) {
    this.schemaName = schemaName;
    this.schemaUrl = schemaUrl;
    this.connection = connection;
  }

  static PreUpgradeFixtureSchema createAtVersion325(String ownerSchemaName) throws SQLException {
    String schemaName = ownerSchemaName + SCHEMA_SUFFIX;
    executeAsAdmin(
        "DROP DATABASE IF EXISTS `" + schemaName + "`",
        "CREATE DATABASE `"
            + schemaName
            + "` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci",
        "GRANT ALL PRIVILEGES ON `" + schemaName + "`.* TO '" + APP_USER + "'@'localhost'",
        "GRANT ALL PRIVILEGES ON `" + schemaName + "`.* TO '" + APP_USER + "'@'127.0.0.1'",
        "FLUSH PRIVILEGES");

    String schemaUrl =
        "jdbc:mysql://" + ADMIN_HOST_AND_PORT + "/" + schemaName + "?connectionTimeZone=UTC";
    Flyway.configure()
        .dataSource(schemaUrl, APP_USER, APP_PASSWORD)
        .locations("classpath:db/migration")
        .target(MigrationVersion.fromVersion(PRE_UPGRADE_BOUNDARY_VERSION))
        .load()
        .migrate();

    Connection connection = DriverManager.getConnection(schemaUrl, APP_USER, APP_PASSWORD);
    return new PreUpgradeFixtureSchema(schemaName, schemaUrl, connection);
  }

  String schemaName() {
    return schemaName;
  }

  /** Open raw JDBC connection into the owned schema, for seeding pre-upgrade fixture rows. */
  Connection connection() {
    return connection;
  }

  /**
   * Opens a new, independent raw JDBC connection into the owned schema (as the ordinary {@code
   * doughnut} app user), separate from {@link #connection()}. Callers own its lifecycle. Intended
   * for reproducing a real interrupted upgrade: execute and commit DDL through the intended
   * interruption point on one connection, close/discard it, then continue on a fresh connection or
   * via a fresh {@link Flyway} instance built from {@link #jdbcUrl()} — mirroring how Flyway's own
   * `repair()`/`migrate()` open their own connections rather than reusing a caller's connection.
   */
  Connection openConnection() throws SQLException {
    return DriverManager.getConnection(schemaUrl, APP_USER, APP_PASSWORD);
  }

  /** JDBC URL for the owned schema, for building a fresh {@link Flyway} instance against it. */
  String jdbcUrl() {
    return schemaUrl;
  }

  /**
   * A fresh {@link FluentConfiguration} pointed at the owned schema with the project's actual
   * migration resources, matching {@link #createAtVersion325}'s configuration. Callers add {@code
   * target(...)} for a bounded migrate call, or load as-is to reach the latest version; each {@code
   * load()} yields an independent {@link Flyway} instance with its own connection handling, the
   * same way the real application's startup migration does.
   */
  FluentConfiguration flywayConfig() {
    return Flyway.configure()
        .dataSource(schemaUrl, APP_USER, APP_PASSWORD)
        .locations("classpath:db/migration");
  }

  /** Closes the raw connection and drops only this owned schema. */
  @Override
  public void close() throws SQLException {
    connection.close();
    executeAsAdmin("DROP DATABASE IF EXISTS `" + schemaName + "`");
  }

  private static void executeAsAdmin(String... sqlStatements) throws SQLException {
    try (Connection admin = DriverManager.getConnection(ADMIN_URL, ADMIN_USER, ADMIN_PASSWORD);
        Statement statement = admin.createStatement()) {
      for (String sql : sqlStatements) {
        statement.execute(sql);
      }
    }
  }
}
