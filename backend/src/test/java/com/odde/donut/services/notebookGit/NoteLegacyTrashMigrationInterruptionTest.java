package com.odde.donut.services.notebookGit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
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
 * Interruption/retry proof for the actual registered {@code V300000326} legacy-trash conversion
 * (delegating to {@link NoteLegacyTrashMigration#run}), extending {@link PreUpgradeFixtureSchema} —
 * the same owned disposable schema harness slices 1-3 use — instead of a second isolation
 * mechanism. A focused new file (rather than a new {@code @Test} on {@link
 * NotebookUpgradeDataPreservationTest}) because this proof's interruption mechanics (a test-owned
 * discarded connection racing the conversion's own transaction boundary, then a real {@code
 * repair()+migrate()} retry) are a different shape from that file's single-pass "run the whole
 * chain once, snapshot before/after" proof, and mirror {@code
 * V300000328DropNoteDeletedAtMigrationTest}'s parameterized-interruption-state style instead.
 *
 * <p><b>Transaction-boundary finding (read {@code NoteLegacyTrashMigration.java} lines 74-89): </b>
 * {@link NoteLegacyTrashMigration#run} performs the <em>whole</em> conversion — every candidate
 * note across every notebook, computed via one {@code candidateNotes} query executed up front —
 * inside a single explicit transaction: {@code connection.setAutoCommit(false)}, then the loop,
 * then exactly one {@code connection.commit()}; any exception rolls the whole thing back. {@code
 * V300000326__MigrateLegacyDeletedNotesToTrash.canExecuteInTransaction()} returns {@code false}
 * specifically so Flyway does not wrap this in a second, outer transaction of its own. There is
 * therefore no reachable "part of the conversion committed, the rest still pending" state for 326 —
 * unlike 328's multi-statement DDL (slice 2), where each {@code ALTER TABLE} is its own MySQL
 * auto-commit. 326's conversion is either entirely committed or entirely rolled back. That is
 * itself the finding this test records, and it shapes the two scenarios proved below instead of
 * inventing a partial-commit case that cannot occur in production:
 *
 * <ul>
 *   <li>{@link RetryState#UNCOMMITTED_MID_TRANSACTION_DISCARDED}: a second connection performs some
 *       of the same kind of conversion writes (trash folder creation, note relocation) with
 *       auto-commit disabled, mirroring {@code run}'s own transaction boundary, then is discarded
 *       without ever calling {@code commit()} — reproducing a crashed/lost connection before the
 *       whole-conversion transaction's single commit point. Proves MySQL/InnoDB rolls the whole
 *       thing back automatically (no orphaned trash folder, no partially-converted note), then that
 *       a real {@code repair()+migrate()} retry completes the conversion correctly and exactly
 *       once.
 *   <li>{@link RetryState#FULL_CONVERSION_COMMITTED_HISTORY_UNRECORDED}: the real {@link
 *       NoteLegacyTrashMigration#run} — the exact method {@code
 *       V300000326__MigrateLegacyDeletedNotesToTrash.migrate} calls — is invoked directly against a
 *       connection that is discarded immediately afterward, without ever going through Flyway. The
 *       conversion is therefore fully committed for real, but {@code flyway_schema_history} has no
 *       row at all for 300000326 — mirroring a process loss after the migration's own commit but
 *       before Flyway's bookkeeping (the same shape as slice 2's {@code
 *       FINAL_DDL_COMMITTED_HISTORY_UNRECORDED} case, for 326 instead of 328). A real {@code
 *       repair()+migrate()} retry is safe by the migration's own documented idempotency ({@code
 *       candidateNotes} only selects rows with {@code deleted_at IS NOT NULL}, and the first,
 *       unrecorded run already cleared it): retry records success without re-touching any row or
 *       re-computing any suffix.
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
class NoteLegacyTrashMigrationInterruptionTest {

  @Autowired DataSource dataSource;

  private enum RetryState {
    UNCOMMITTED_MID_TRANSACTION_DISCARDED,
    FULL_CONVERSION_COMMITTED_HISTORY_UNRECORDED
  }

  @ParameterizedTest
  @EnumSource(RetryState.class)
  void retryingAnInterruptedLegacyTrashConversionEndsCorrectExactlyOnce(RetryState state)
      throws Exception {
    String ownerSchemaName;
    try (Connection connection = dataSource.getConnection()) {
      ownerSchemaName = connection.getCatalog();
    }

    try (PreUpgradeFixtureSchema fixture =
        PreUpgradeFixtureSchema.createAtVersion325(ownerSchemaName)) {
      Connection connection = fixture.connection();
      FixtureIds ids = seedFixture(connection);

      switch (state) {
        case UNCOMMITTED_MID_TRANSACTION_DISCARDED -> {
          interruptBeforeAnyCommit(fixture, ids);
          assertInterruptedWriteWasRolledBack(connection, ids);
          assertNoHistoryRowFor326(connection);
        }
        case FULL_CONVERSION_COMMITTED_HISTORY_UNRECORDED -> {
          runRealConversionOutsideFlyway(fixture);
          assertConversionCompleteExactlyOnce(connection, ids);
          assertNoHistoryRowFor326(connection);
        }
      }

      Flyway flyway =
          fixture.flywayConfig().target(MigrationVersion.fromVersion("300000327")).load();
      flyway.repair();
      flyway.migrate();

      assertConversionCompleteExactlyOnce(connection, ids);
      assertExactlyOneSuccessfulHistoryRowFor326(connection);
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Interruption scenarios
  // ---------------------------------------------------------------------------------------------

  /**
   * Opens a second connection, disables auto-commit (mirroring {@link
   * NoteLegacyTrashMigration#run}'s own transaction boundary), performs writes of the same shape
   * the real conversion would make for one note (trash root + child folder creation, note
   * relocation), then discards the connection without ever calling {@code commit()} — reproducing a
   * crashed/lost connection before the whole-conversion transaction's single commit point.
   */
  private void interruptBeforeAnyCommit(PreUpgradeFixtureSchema fixture, FixtureIds ids)
      throws SQLException {
    try (Connection interrupted = fixture.openConnection()) {
      interrupted.setAutoCommit(false);
      int trashRootId = insertFolder(interrupted, ids.notebookOne(), null, "_trash");
      int trashDocsId = insertFolder(interrupted, ids.notebookOne(), trashRootId, "Docs");
      try (PreparedStatement statement =
          interrupted.prepareStatement(
              "UPDATE note SET folder_id = ?, deleted_at = NULL, updated_at = ? WHERE id = ?")) {
        statement.setInt(1, trashDocsId);
        statement.setTimestamp(2, Timestamp.from(Instant.now()));
        statement.setInt(3, ids.alphaNoteId());
        statement.executeUpdate();
      }
      // Never committed; try-with-resources closes `interrupted` here without commit or explicit
      // rollback, reproducing a dropped connection. MySQL/InnoDB rolls the whole transaction back.
    }
  }

  /**
   * Invokes the real production conversion entry point directly — the exact method Flyway's {@code
   * V300000326} migration calls — against a connection that is then discarded without ever running
   * Flyway, so the conversion fully commits for real but Flyway never records it.
   */
  private void runRealConversionOutsideFlyway(PreUpgradeFixtureSchema fixture) throws SQLException {
    try (Connection interrupted = fixture.openConnection()) {
      NoteLegacyTrashMigration.run(interrupted, Instant.now());
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Fixture seeding
  // ---------------------------------------------------------------------------------------------

  private record FixtureIds(
      int notebookOne,
      int notebookTwo,
      int docsFolderId,
      int trashRootTwoFolderId,
      int alphaNoteId,
      int betaNoteId,
      int draftNoteId,
      int existingTrashNoteId,
      int keepNoteId) {}

  private FixtureIds seedFixture(Connection connection) throws SQLException {
    int ownerOne = insertUser(connection, "Owner One", "owner-one");
    int ownerTwo = insertUser(connection, "Owner Two", "owner-two");

    int notebookOne = insertNotebook(connection, ownerOne, "Notebook One");
    int notebookTwo = insertNotebook(connection, ownerTwo, "Notebook Two");

    // Notebook One: a live folder with one legacy-deleted note (forces trash-root + child-folder
    // creation to replicate the folder trail) and one active note left untouched.
    int docs = insertFolder(connection, notebookOne, null, "Docs");
    int alpha =
        insertNote(connection, notebookOne, docs, "Alpha", "Alpha content", legacyDeletedAt());
    int beta = insertNote(connection, notebookOne, docs, "Beta", "Beta content", null);

    // Notebook Two: a pre-existing _trash root already containing a note titled "Draft" (the exact
    // destination a legacy-deleted note with no original folder will convert to), forcing a genuine
    // one-time collision suffix ("Draft (2)") — the same collision shape slice 3 uses, since the
    // DB's
    // own uk_note_notebook_folder_title constraint (baseline schema, applies regardless of
    // deleted_at) rules out two same-titled notes sharing one original folder. One active note is
    // left untouched.
    int trashRootTwo = insertFolder(connection, notebookTwo, null, "_trash");
    int existingTrashNote =
        insertNote(
            connection, notebookTwo, trashRootTwo, "Draft", "Pre-existing trashed draft", null);
    int draft =
        insertNote(connection, notebookTwo, null, "Draft", "Legacy draft", legacyDeletedAt());
    int keep = insertNote(connection, notebookTwo, null, "Keep", "Keep content", null);

    return new FixtureIds(
        notebookOne, notebookTwo, docs, trashRootTwo, alpha, beta, draft, existingTrashNote, keep);
  }

  private static Timestamp legacyDeletedAt() {
    return Timestamp.valueOf("2019-01-01 00:00:00");
  }

  private static Timestamp seedTimestamp() {
    return Timestamp.valueOf("2020-06-01 00:00:00");
  }

  private int insertUser(Connection connection, String name, String externalIdentifier)
      throws SQLException {
    return insertReturningId(
        connection,
        "INSERT INTO user (name, external_identifier) VALUES ('"
            + name
            + "', '"
            + externalIdentifier
            + "')");
  }

  private int insertNotebook(Connection connection, int ownerId, String name) throws SQLException {
    int ownershipId =
        insertReturningId(connection, "INSERT INTO ownership (user_id) VALUES (" + ownerId + ")");
    return insertReturningId(
        connection,
        "INSERT INTO notebook (ownership_id, creator_id, name, created_at, updated_at) VALUES ("
            + ownershipId
            + ", "
            + ownerId
            + ", '"
            + name
            + "', '"
            + seedTimestamp()
            + "', '"
            + seedTimestamp()
            + "')");
  }

  private int insertFolder(
      Connection connection, int notebookId, Integer parentFolderId, String name)
      throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement(
            "INSERT INTO folder (notebook_id, parent_folder_id, name, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, ?)",
            Statement.RETURN_GENERATED_KEYS)) {
      statement.setInt(1, notebookId);
      if (parentFolderId == null) {
        statement.setNull(2, java.sql.Types.INTEGER);
      } else {
        statement.setInt(2, parentFolderId);
      }
      statement.setString(3, name);
      statement.setTimestamp(4, seedTimestamp());
      statement.setTimestamp(5, seedTimestamp());
      statement.executeUpdate();
      try (ResultSet keys = statement.getGeneratedKeys()) {
        keys.next();
        return keys.getInt(1);
      }
    }
  }

  private int insertNote(
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

  private int insertReturningId(Connection connection, String sql) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute(sql, Statement.RETURN_GENERATED_KEYS);
      try (ResultSet keys = statement.getGeneratedKeys()) {
        keys.next();
        return keys.getInt(1);
      }
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Assertions
  // ---------------------------------------------------------------------------------------------

  private void assertInterruptedWriteWasRolledBack(Connection connection, FixtureIds ids)
      throws SQLException {
    assertThat(
        "no orphaned trash folder from the discarded connection",
        folderCount(connection, ids.notebookOne(), "_trash"),
        equalTo(0));
    NoteState alpha = readNote(connection, ids.alphaNoteId());
    assertThat(
        "interrupted note relocation rolled back", alpha.folderId(), equalTo(ids.docsFolderId()));
    assertThat("interrupted deleted_at clear rolled back", alpha.deletedAt(), is(not(nullValue())));
  }

  private void assertConversionCompleteExactlyOnce(Connection connection, FixtureIds ids)
      throws SQLException {
    // Notebook One: Alpha relocated into a single _trash/Docs subtree, no title change needed.
    assertThat(
        "exactly one _trash root for notebook one",
        folderCount(connection, ids.notebookOne(), "_trash"),
        equalTo(1));
    int trashRootOne = folderId(connection, ids.notebookOne(), "_trash", null);
    assertThat(
        "exactly one _trash/Docs subfolder, not duplicated by retry",
        childFolderCount(connection, ids.notebookOne(), trashRootOne, "Docs"),
        equalTo(1));
    int trashDocsOne = folderId(connection, ids.notebookOne(), "Docs", trashRootOne);

    NoteState alpha = readNote(connection, ids.alphaNoteId());
    assertThat(
        "Alpha relocated to _trash/Docs exactly once", alpha.folderId(), equalTo(trashDocsOne));
    assertThat("Alpha deleted_at cleared", alpha.deletedAt(), is(nullValue()));
    assertThat("Alpha keeps its own title, no spurious suffix", alpha.title(), equalTo("Alpha"));

    NoteState beta = readNote(connection, ids.betaNoteId());
    assertThat(
        "untouched active note stays in place", beta.folderId(), equalTo(ids.docsFolderId()));
    assertThat("untouched active note stays undeleted", beta.deletedAt(), is(nullValue()));

    // Notebook Two: the legacy-deleted Draft relocates into the pre-existing _trash root (reused,
    // never a second one created by a retry), colliding with the note already occupying "Draft"
    // there and getting exactly one collision suffix.
    assertThat(
        "the pre-existing _trash root is reused, not duplicated by a retry",
        folderCount(connection, ids.notebookTwo(), "_trash"),
        equalTo(1));
    int trashRootTwo = folderId(connection, ids.notebookTwo(), "_trash", null);
    assertThat(
        "pre-existing _trash root identity preserved",
        trashRootTwo,
        equalTo(ids.trashRootTwoFolderId()));

    NoteState draft = readNote(connection, ids.draftNoteId());
    assertThat(draft.folderId(), equalTo(trashRootTwo));
    assertThat(draft.deletedAt(), is(nullValue()));
    assertThat(
        "legacy draft gets exactly one collision suffix, never re-suffixed by a retry",
        draft.title(),
        equalTo("Draft (2)"));

    NoteState existingTrashNote = readNote(connection, ids.existingTrashNoteId());
    assertThat(
        "the note that already occupied the destination stays completely untouched",
        existingTrashNote.title(),
        equalTo("Draft"));
    assertThat(existingTrashNote.folderId(), equalTo(trashRootTwo));
    assertThat(existingTrashNote.deletedAt(), is(nullValue()));

    NoteState keep = readNote(connection, ids.keepNoteId());
    assertThat("untouched active note stays unbothered", keep.folderId(), is(nullValue()));
    assertThat(keep.deletedAt(), is(nullValue()));
  }

  private record NoteState(Integer folderId, String title, Timestamp deletedAt) {}

  private NoteState readNote(Connection connection, int noteId) throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement("SELECT folder_id, title, deleted_at FROM note WHERE id = ?")) {
      statement.setInt(1, noteId);
      try (ResultSet rs = statement.executeQuery()) {
        rs.next();
        int folderId = rs.getInt("folder_id");
        return new NoteState(
            rs.wasNull() ? null : folderId, rs.getString("title"), rs.getTimestamp("deleted_at"));
      }
    }
  }

  private int folderCount(Connection connection, int notebookId, String name) throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement(
            "SELECT COUNT(*) AS c FROM folder WHERE notebook_id = ? AND parent_folder_id IS NULL"
                + " AND name = ?")) {
      statement.setInt(1, notebookId);
      statement.setString(2, name);
      try (ResultSet rs = statement.executeQuery()) {
        rs.next();
        return rs.getInt("c");
      }
    }
  }

  private int childFolderCount(
      Connection connection, int notebookId, int parentFolderId, String name) throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement(
            "SELECT COUNT(*) AS c FROM folder WHERE notebook_id = ? AND parent_folder_id = ?"
                + " AND name = ?")) {
      statement.setInt(1, notebookId);
      statement.setInt(2, parentFolderId);
      statement.setString(3, name);
      try (ResultSet rs = statement.executeQuery()) {
        rs.next();
        return rs.getInt("c");
      }
    }
  }

  private int folderId(Connection connection, int notebookId, String name, Integer parentFolderId)
      throws SQLException {
    String sql =
        "SELECT id FROM folder WHERE notebook_id = ? AND name = ? AND "
            + (parentFolderId == null ? "parent_folder_id IS NULL" : "parent_folder_id = ?");
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, notebookId);
      statement.setString(2, name);
      if (parentFolderId != null) {
        statement.setInt(3, parentFolderId);
      }
      try (ResultSet rs = statement.executeQuery()) {
        rs.next();
        return rs.getInt("id");
      }
    }
  }

  private void assertNoHistoryRowFor326(Connection connection) throws SQLException {
    try (Statement statement = connection.createStatement();
        ResultSet rs =
            statement.executeQuery(
                "SELECT COUNT(*) AS c FROM flyway_schema_history WHERE version = '300000326'")) {
      rs.next();
      assertThat(
          "no Flyway history row yet for 300000326 in this scenario", rs.getInt("c"), equalTo(0));
    }
  }

  private void assertExactlyOneSuccessfulHistoryRowFor326(Connection connection)
      throws SQLException {
    try (Statement statement = connection.createStatement();
        ResultSet resultSet =
            statement.executeQuery(
                "SELECT success FROM flyway_schema_history WHERE version = '300000326' "
                    + "ORDER BY installed_rank ASC")) {
      List<Boolean> successFlags = new ArrayList<>();
      while (resultSet.next()) {
        successFlags.add(resultSet.getBoolean("success"));
      }
      assertThat(successFlags, equalTo(List.of(true)));
    }
  }
}
