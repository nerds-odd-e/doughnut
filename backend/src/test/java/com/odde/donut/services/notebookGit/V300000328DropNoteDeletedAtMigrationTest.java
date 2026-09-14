package com.odde.donut.services.notebookGit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Flyway-boundary proof for the revised {@code V300000328__drop_note_deleted_at.sql}: runs the
 * actual registered migration resource (not a copy) against {@link PreUpgradeFixtureSchema}'s
 * owned, populated pre-upgrade schema, from every schema shape reachable by interrupting this
 * migration's DDL — including after a statement's commit but before Flyway records success — using
 * a real discarded-connection interruption rather than mocked Flyway internals.
 *
 * <p>Before this migration was revised, retrying it after {@code idx_note_structural_peer} had
 * already been dropped reproduced MySQL error 1091 ("Can't DROP 'idx_note_structural_peer'; check
 * that column/key exists"). That reproduction used the migration's original (pre-revision) SQL
 * directly against MySQL and is not re-executed here, since the resource under test is now the
 * fixed migration.
 */
@SpringBootTest
@ActiveProfiles("test")
class V300000328DropNoteDeletedAtMigrationTest {

  private static final String FINAL_INDEX_DDL =
      "ALTER TABLE note ADD INDEX idx_note_structural_peer (notebook_id, folder_id, id)";

  @Autowired DataSource dataSource;

  private enum RetryState {
    /** No interruption: the ordinary, uninterrupted upgrade path. */
    ORIGINAL_SCHEMA,
    /** Interrupted after the index drop committed; column still present. */
    INDEX_DROPPED_ONLY,
    /** Interrupted after the column drop committed; final index not yet recreated. */
    INDEX_AND_COLUMN_DROPPED,
    /** Complete DDL committed, but Flyway's success history was never recorded. */
    FINAL_DDL_COMMITTED_HISTORY_UNRECORDED,
    /** Migration already applied and recorded successful; retry must stay a no-op. */
    ALREADY_RECORDED_SUCCESS,
    /**
     * The guard procedure's own CREATE committed (or its CALL succeeded) but the interruption hit
     * before the final DROP PROCEDURE committed, leaving the procedure itself behind while the
     * table is still in its original, unmigrated shape.
     */
    GUARD_PROCEDURE_LEFT_BEHIND_FROM_INTERRUPTED_ATTEMPT
  }

  @ParameterizedTest
  @EnumSource(RetryState.class)
  void retryingV300000328FromEveryReachableStateEndsCorrectWithRowsPreserved(RetryState state)
      throws Exception {
    String ownerSchemaName;
    try (Connection connection = dataSource.getConnection()) {
      ownerSchemaName = connection.getCatalog();
    }

    try (PreUpgradeFixtureSchema fixture =
        PreUpgradeFixtureSchema.createAtVersion325(ownerSchemaName)) {
      seedNoteRows(fixture.connection());
      List<NoteRow> notesBefore = readNoteRows(fixture.connection());

      // Run the actual registered chain through V300000327 for real (legacy-trash conversion,
      // baseline rebuild); both are no-ops here since no note is legacy-deleted and no notebook has
      // a Git binding, but this exercises the real Flyway ordering instead of jumping straight to
      // 328.
      fixture.flywayConfig().target(MigrationVersion.fromVersion("300000327")).load().migrate();

      switch (state) {
        case ORIGINAL_SCHEMA -> fixture.flywayConfig().load().migrate();
        case ALREADY_RECORDED_SUCCESS -> {
          fixture.flywayConfig().load().migrate();
          repairAndMigrate(fixture);
        }
        case GUARD_PROCEDURE_LEFT_BEHIND_FROM_INTERRUPTED_ATTEMPT -> {
          leaveStaleGuardProcedureBehind(fixture);
          repairAndMigrate(fixture);
        }
        default -> {
          interruptAfterCommittingPartialDdl(fixture, state);
          repairAndMigrate(fixture);
        }
      }

      assertFinalSchema(fixture.connection());
      assertThat(readNoteRows(fixture.connection()), equalTo(notesBefore));
      assertExactlyOneSuccessfulHistoryRow(fixture.connection());
    }
  }

  /**
   * Simulates a crashed/lost connection: opens an independent connection, executes and commits DDL
   * only through the intended interruption point, then closes it without ever going through Flyway
   * — so Flyway's history has no row at all for V300000328, matching a process loss after a DDL
   * statement's implicit MySQL commit but before Flyway could record anything.
   */
  private void interruptAfterCommittingPartialDdl(PreUpgradeFixtureSchema fixture, RetryState state)
      throws SQLException {
    try (Connection interrupted = fixture.openConnection();
        Statement statement = interrupted.createStatement()) {
      statement.execute("ALTER TABLE note DROP INDEX idx_note_structural_peer");
      if (state == RetryState.INDEX_DROPPED_ONLY) {
        return;
      }
      statement.execute("ALTER TABLE note DROP COLUMN deleted_at");
      if (state == RetryState.INDEX_AND_COLUMN_DROPPED) {
        return;
      }
      statement.execute(FINAL_INDEX_DDL);
    }
    // `interrupted` is now closed/discarded; repairAndMigrate opens its own fresh connection(s).
  }

  /**
   * Simulates a crash between the guard procedure's CREATE PROCEDURE and its final DROP PROCEDURE —
   * both individually DDL-auto-committed in MySQL — by leaving a stand-in procedure of the same
   * name behind while the table itself stays untouched. Proves the migration's leading {@code DROP
   * PROCEDURE IF EXISTS} clears a stale procedure from any earlier interrupted attempt, regardless
   * of where between CREATE/CALL/DROP that attempt was lost, before re-deriving the correct branch
   * from the table's actual schema shape.
   */
  private void leaveStaleGuardProcedureBehind(PreUpgradeFixtureSchema fixture) throws SQLException {
    try (Connection interrupted = fixture.openConnection();
        Statement statement = interrupted.createStatement()) {
      statement.execute(
          "CREATE PROCEDURE `_v300000328_drop_note_deleted_at`() BEGIN SELECT 1; END");
    }
    // `interrupted` is now closed/discarded; the table is still in its original, unmigrated shape.
  }

  private void repairAndMigrate(PreUpgradeFixtureSchema fixture) {
    Flyway flyway = fixture.flywayConfig().load();
    flyway.repair();
    flyway.migrate();
  }

  private void seedNoteRows(Connection connection) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute(
          "INSERT INTO note (title, content) VALUES "
              + "('Fixture Note One', 'Preserved content one'), "
              + "('Fixture Note Two', 'Preserved content two')");
    }
  }

  private List<NoteRow> readNoteRows(Connection connection) throws SQLException {
    List<NoteRow> rows = new ArrayList<>();
    try (Statement statement = connection.createStatement();
        ResultSet resultSet =
            statement.executeQuery("SELECT id, title, content FROM note ORDER BY id ASC")) {
      while (resultSet.next()) {
        rows.add(
            new NoteRow(
                resultSet.getInt("id"),
                resultSet.getString("title"),
                resultSet.getString("content")));
      }
    }
    return rows;
  }

  private void assertFinalSchema(Connection connection) throws SQLException {
    try (Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SHOW COLUMNS FROM note LIKE 'deleted_at'")) {
      assertThat("note.deleted_at must be gone", resultSet.next(), is(false));
    }
    try (Statement statement = connection.createStatement();
        ResultSet resultSet =
            statement.executeQuery(
                "SELECT GROUP_CONCAT(column_name ORDER BY seq_in_index) AS cols "
                    + "FROM information_schema.statistics "
                    + "WHERE table_schema = DATABASE() AND table_name = 'note' "
                    + "AND index_name = 'idx_note_structural_peer'")) {
      assertThat(resultSet.next(), is(true));
      assertThat(resultSet.getString("cols"), equalTo("notebook_id,folder_id,id"));
    }
  }

  private void assertExactlyOneSuccessfulHistoryRow(Connection connection) throws SQLException {
    try (Statement statement = connection.createStatement();
        ResultSet resultSet =
            statement.executeQuery(
                "SELECT success FROM flyway_schema_history WHERE version = '300000328' "
                    + "ORDER BY installed_rank ASC")) {
      List<Boolean> successFlags = new ArrayList<>();
      while (resultSet.next()) {
        successFlags.add(resultSet.getBoolean("success"));
      }
      assertThat(successFlags, equalTo(List.of(true)));
    }
  }

  private record NoteRow(int id, String title, String content) {}
}
