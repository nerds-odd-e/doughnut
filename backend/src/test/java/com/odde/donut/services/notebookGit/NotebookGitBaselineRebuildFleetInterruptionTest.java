package com.odde.donut.services.notebookGit;

import static com.odde.donut.services.notebookGit.NotebookGitRebuildTestSupport.currentPortableTreeFromDb;
import static com.odde.donut.services.notebookGit.NotebookGitRebuildTestSupport.readTreeEntries;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import com.odde.donut.services.notebookExport.PortableTreeEntry;
import com.odde.donut.testability.GitBundleTestReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import javax.sql.DataSource;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.test.context.ActiveProfiles;

/**
 * Interruption/retry proof for the actual registered {@code V300000327} notebook Git baseline
 * rebuild (delegating to {@link NotebookGitBaselineRebuild#rebuildNotebook}), extending {@link
 * PreUpgradeFixtureSchema} — the same owned disposable schema harness slices 1-4 use — instead of a
 * second isolation mechanism.
 *
 * <p><b>Transaction-boundary finding (read {@code V300000327__RebuildNotebookGitBaselines.java} and
 * {@code NotebookGitBaselineRebuild.java}):</b> {@code V300000327__RebuildNotebookGitBaselines}
 * queries every live ({@code deleted_at IS NULL}) already-bound notebook ordered by {@code id ASC},
 * then calls {@link NotebookGitBaselineRebuild#rebuildNotebook} once per notebook in a plain Java
 * loop, on the <em>same</em> connection Flyway hands to the migration. {@code
 * canExecuteInTransaction()} returns {@code false} (mirroring 326's own opt-out) specifically so
 * Flyway does not wrap the whole loop in one outer transaction — each notebook manages its own
 * commit instead. Inside {@code rebuildNotebook}: {@code connection.setAutoCommit(false)}, build
 * the replacement tree/bundle, one {@code UPDATE notebook_git_binding ...}, then exactly one {@code
 * connection.commit()}; any exception rolls back just that notebook's UPDATE and rethrows (killing
 * the whole migration run, per the class javadoc: "A failure on one notebook fails the migration
 * fast"). So a fleet of N notebooks really does mean N independent commits on one migration run:
 * interrupting after notebook K's commit but before notebook K+1 starts leaves a genuinely
 * reachable partial-fleet state (K rebuilt, K+1..N still on their old binding), and interrupting
 * before a notebook's own single UPDATE commits leaves that notebook's binding completely unchanged
 * (InnoDB rolls back the lone UPDATE). The migration keeps no per-notebook checkpoint: a retry
 * re-queries every live bound notebook and rebuilds all of them again, unconditionally — the class
 * javadoc calls this "simply replaced again idempotently" — so an already-correct notebook is
 * safely re-processed (new head/commit timestamp, same tree/content), never skipped and never
 * corrupted. That reachable-state shape, not a hypothetical partial-UPDATE state, is what the three
 * notebooks and two interruption mechanics below reproduce:
 *
 * <ul>
 *   <li><b>notebookFullyCommittedBeforeInterruption:</b> {@link
 *       NotebookGitBaselineRebuild#rebuildNotebook} is invoked directly, on a discarded connection,
 *       outside Flyway entirely — the exact method the migration calls for one notebook — so its
 *       binding is genuinely, fully committed, but {@code flyway_schema_history} has no row yet for
 *       300000327. Reproduces "notebook K's rebuild commits" from the story's key example.
 *   <li><b>notebookInterruptedMidRebuild:</b> a second connection performs the same single {@code
 *       UPDATE notebook_git_binding ...} shape {@code rebuildNotebook} makes, with auto-commit
 *       disabled, then is discarded without ever calling {@code commit()} — reproducing a crashed
 *       connection before that notebook's own commit point. Reproduces "a later notebook's rebuild
 *       is interrupted" mid-transaction.
 *   <li><b>notebookNeverReached:</b> left completely untouched — reproduces "between notebook
 *       baseline rebuilds," the story's key example #2, for the notebook the fleet never got to
 *       before the process was lost.
 * </ul>
 *
 * <p>After a real {@code repair()+migrate()} retry (through 328), every one of the three notebooks
 * must end with a complete, correct tree and a matching recorded head — compared against actual
 * bundle/DB content, never against the old (pre-retry) Git SHA or rebuild timestamp, since a
 * correctly-retried notebook is expected to get a brand new head either way.
 */
@SpringBootTest
@ActiveProfiles("test")
class NotebookGitBaselineRebuildFleetInterruptionTest {

  @Autowired DataSource dataSource;

  @Test
  void retryingAPartiallyRebuiltNotebookFleetEndsEveryBindingCorrectExactlyOnce() throws Exception {
    String ownerSchemaName;
    try (Connection connection = dataSource.getConnection()) {
      ownerSchemaName = connection.getCatalog();
    }

    try (PreUpgradeFixtureSchema fixture =
        PreUpgradeFixtureSchema.createAtVersion325(ownerSchemaName)) {
      Connection connection = fixture.connection();
      FixtureIds ids = seedFixture(connection);

      // Run the actual registered chain up to (and including) 326 for real, so 327's retry below
      // starts from the same state a real operator retry would: 326 already successfully recorded.
      fixture.flywayConfig().target(MigrationVersion.fromVersion("300000326")).load().migrate();

      BindingSnapshot originalCommitted = readBinding(connection, ids.notebookCommitted());
      BindingSnapshot originalInterruptedMid =
          readBinding(connection, ids.notebookInterruptedMid());
      BindingSnapshot originalNeverReached = readBinding(connection, ids.notebookNeverReached());

      // Simulate a partially rebuilt fleet: notebookCommitted's rebuild genuinely commits (K),
      // notebookInterruptedMid's own rebuild is interrupted before its commit (K+1, mid-rebuild),
      // notebookNeverReached is never touched (K+2, "between notebook baseline rebuilds").
      rebuildNotebookDirectlyOutsideFlyway(
          fixture, ids.notebookCommitted(), Instant.parse("2026-09-13T09:00:00Z"));
      interruptNotebookMidRebuild(fixture, ids.notebookInterruptedMid());

      assertNoHistoryRowFor327(connection);

      BindingSnapshot committedBeforeRetry = readBinding(connection, ids.notebookCommitted());
      assertThat(
          "the directly-invoked rebuild genuinely committed a new binding",
          committedBeforeRetry.acceptedGitObjectId(),
          not(equalTo(originalCommitted.acceptedGitObjectId())));

      BindingSnapshot interruptedMidBeforeRetry =
          readBinding(connection, ids.notebookInterruptedMid());
      assertThat(
          "the interrupted mid-rebuild UPDATE never committed; InnoDB rolled it back",
          interruptedMidBeforeRetry.acceptedGitObjectId(),
          equalTo(originalInterruptedMid.acceptedGitObjectId()));
      assertArrayEquals(
          originalInterruptedMid.bundleBytes(), interruptedMidBeforeRetry.bundleBytes());

      BindingSnapshot neverReachedBeforeRetry = readBinding(connection, ids.notebookNeverReached());
      assertThat(
          "the never-reached notebook's binding is completely untouched before retry",
          neverReachedBeforeRetry.acceptedGitObjectId(),
          equalTo(originalNeverReached.acceptedGitObjectId()));

      // The operator's real recovery: repair + migrate through the latest registered version (328).
      Flyway flyway = fixture.flywayConfig().load();
      flyway.repair();
      flyway.migrate();

      assertExactlyOneSuccessfulHistoryRowFor(connection, "300000327");
      assertExactlyOneSuccessfulHistoryRowFor(connection, "300000328");

      // Every selected notebook now has a complete, correct tree and a matching recorded head,
      // regardless of which interruption state it started retry from.
      assertBindingRebuiltCorrectly(connection, ids.notebookCommitted());
      assertBindingRebuiltCorrectly(connection, ids.notebookInterruptedMid());
      assertBindingRebuiltCorrectly(connection, ids.notebookNeverReached());

      // Retained notebook data (notes/folders) is present and correct for each notebook.
      assertThat(
          readNoteContent(connection, ids.noteCommittedId()), equalTo("Content for Committed"));
      assertThat(
          readNoteContent(connection, ids.noteInterruptedMidId()),
          equalTo("Content for InterruptedMid"));
      assertThat(
          readNoteContent(connection, ids.noteNeverReachedId()),
          equalTo("Content for NeverReached"));
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Interruption scenarios
  // ---------------------------------------------------------------------------------------------

  /**
   * Invokes the real production entry point directly — the exact method {@code
   * V300000327__RebuildNotebookGitBaselines} calls for one notebook — against a connection that is
   * discarded immediately afterward, so the rebuild fully commits for real but Flyway never records
   * 300000327 and never reaches the next notebook in the fleet.
   */
  private void rebuildNotebookDirectlyOutsideFlyway(
      PreUpgradeFixtureSchema fixture, int notebookId, Instant rebuildTime) throws Exception {
    try (Connection interrupted = fixture.openConnection()) {
      NotebookGitBaselineRebuild.rebuildNotebook(interrupted, notebookId, rebuildTime);
    }
  }

  /**
   * Opens a second connection, disables auto-commit (mirroring {@link
   * NotebookGitBaselineRebuild#rebuildNotebook}'s own transaction boundary), performs the same
   * single-UPDATE write shape that method makes, then discards the connection without ever calling
   * {@code commit()} — reproducing a crashed connection before that notebook's own rebuild commits.
   */
  private void interruptNotebookMidRebuild(PreUpgradeFixtureSchema fixture, int notebookId)
      throws SQLException {
    try (Connection interrupted = fixture.openConnection()) {
      interrupted.setAutoCommit(false);
      try (PreparedStatement statement =
          interrupted.prepareStatement(
              "UPDATE notebook_git_binding SET accepted_git_object_id = ?, bundle_bytes = ?,"
                  + " updated_at = ? WHERE notebook_id = ?")) {
        statement.setString(1, "f".repeat(40));
        statement.setBytes(2, new byte[] {9, 9, 9});
        statement.setTimestamp(3, Timestamp.from(Instant.now()));
        statement.setInt(4, notebookId);
        statement.executeUpdate();
      }
      // Never committed; try-with-resources closes `interrupted` here without commit or explicit
      // rollback, reproducing a dropped connection. MySQL/InnoDB rolls the whole UPDATE back.
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Fixture seeding
  // ---------------------------------------------------------------------------------------------

  private record FixtureIds(
      int notebookCommitted,
      int notebookInterruptedMid,
      int notebookNeverReached,
      int noteCommittedId,
      int noteInterruptedMidId,
      int noteNeverReachedId) {}

  private FixtureIds seedFixture(Connection connection) throws SQLException {
    int ownerCommitted = insertUser(connection, "Owner Committed", "owner-committed");
    int ownerInterruptedMid = insertUser(connection, "Owner InterruptedMid", "owner-interrupted");
    int ownerNeverReached = insertUser(connection, "Owner NeverReached", "owner-never-reached");

    // Ids increase in this order (auto-increment), matching V300000327's own `ORDER BY n.id ASC`
    // fleet processing order, so "K committed, K+1 interrupted, K+2 never reached" is reproduced
    // exactly as the fleet would encounter them.
    int notebookCommitted = insertNotebook(connection, ownerCommitted, "Committed Notebook");
    int notebookInterruptedMid =
        insertNotebook(connection, ownerInterruptedMid, "InterruptedMid Notebook");
    int notebookNeverReached =
        insertNotebook(connection, ownerNeverReached, "NeverReached Notebook");

    int folderCommitted = insertFolder(connection, notebookCommitted, "Folder Committed");
    int noteCommitted =
        insertNote(
            connection,
            notebookCommitted,
            folderCommitted,
            "Note Committed",
            "Content for Committed");

    int folderInterruptedMid =
        insertFolder(connection, notebookInterruptedMid, "Folder InterruptedMid");
    int noteInterruptedMid =
        insertNote(
            connection,
            notebookInterruptedMid,
            folderInterruptedMid,
            "Note InterruptedMid",
            "Content for InterruptedMid");

    int folderNeverReached = insertFolder(connection, notebookNeverReached, "Folder NeverReached");
    int noteNeverReached =
        insertNote(
            connection,
            notebookNeverReached,
            folderNeverReached,
            "Note NeverReached",
            "Content for NeverReached");

    // Every notebook starts with a pre-existing, distinguishable stale binding so the fleet rebuild
    // has something real to replace, and so "unchanged" vs. "replaced" is observable.
    insertBinding(connection, notebookCommitted, "1".repeat(40), new byte[] {1});
    insertBinding(connection, notebookInterruptedMid, "2".repeat(40), new byte[] {2});
    insertBinding(connection, notebookNeverReached, "3".repeat(40), new byte[] {3});

    return new FixtureIds(
        notebookCommitted,
        notebookInterruptedMid,
        notebookNeverReached,
        noteCommitted,
        noteInterruptedMid,
        noteNeverReached);
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

  private int insertFolder(Connection connection, int notebookId, String name) throws SQLException {
    return insertReturningId(
        connection,
        "INSERT INTO folder (notebook_id, parent_folder_id, name, created_at, updated_at) VALUES ("
            + notebookId
            + ", NULL, '"
            + name
            + "', '"
            + seedTimestamp()
            + "', '"
            + seedTimestamp()
            + "')");
  }

  private int insertNote(
      Connection connection, int notebookId, int folderId, String title, String content)
      throws SQLException {
    return insertReturningId(
        connection,
        "INSERT INTO note (notebook_id, folder_id, title, content, created_at, updated_at) VALUES ("
            + notebookId
            + ", "
            + folderId
            + ", '"
            + title
            + "', '"
            + content
            + "', '"
            + seedTimestamp()
            + "', '"
            + seedTimestamp()
            + "')");
  }

  private void insertBinding(
      Connection connection, int notebookId, String gitObjectId, byte[] bundleBytes)
      throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement(
            "INSERT INTO notebook_git_binding (notebook_id, accepted_git_object_id, bundle_bytes,"
                + " created_at, updated_at) VALUES (?, ?, ?, ?, ?)")) {
      statement.setInt(1, notebookId);
      statement.setString(2, gitObjectId);
      statement.setBytes(3, bundleBytes);
      statement.setTimestamp(4, seedTimestamp());
      statement.setTimestamp(5, seedTimestamp());
      statement.executeUpdate();
    }
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

  private record BindingSnapshot(String acceptedGitObjectId, byte[] bundleBytes) {}

  private BindingSnapshot readBinding(Connection connection, int notebookId) throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement(
            "SELECT accepted_git_object_id, bundle_bytes FROM notebook_git_binding"
                + " WHERE notebook_id = ?")) {
      statement.setInt(1, notebookId);
      try (ResultSet rs = statement.executeQuery()) {
        assertThat("a binding row must exist for notebook " + notebookId, rs.next(), is(true));
        return new BindingSnapshot(
            rs.getString("accepted_git_object_id"), rs.getBytes("bundle_bytes"));
      }
    }
  }

  private String readNoteContent(Connection connection, int noteId) throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement("SELECT content FROM note WHERE id = ?")) {
      statement.setInt(1, noteId);
      try (ResultSet rs = statement.executeQuery()) {
        rs.next();
        return rs.getString("content");
      }
    }
  }

  private void assertNoHistoryRowFor327(Connection connection) throws SQLException {
    try (Statement statement = connection.createStatement();
        ResultSet rs =
            statement.executeQuery(
                "SELECT COUNT(*) AS c FROM flyway_schema_history WHERE version = '300000327'")) {
      rs.next();
      assertThat(
          "no Flyway history row yet for 300000327 before the real retry",
          rs.getInt("c"),
          equalTo(0));
    }
  }

  private void assertExactlyOneSuccessfulHistoryRowFor(Connection connection, String version)
      throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement(
            "SELECT success FROM flyway_schema_history WHERE version = ? ORDER BY installed_rank"
                + " ASC")) {
      statement.setString(1, version);
      try (ResultSet rs = statement.executeQuery()) {
        assertThat("exactly one history row for " + version, rs.next(), is(true));
        assertThat(rs.getBoolean("success"), is(true));
        assertThat("no second history row for " + version, rs.next(), is(false));
      }
    }
  }

  /**
   * Verifies the notebook's binding ends in a complete, correct state: a recorded head that matches
   * the bundle's actual advertised head, a single parentless root commit, and a tree matching the
   * notebook's current database content — reused from {@link NotebookGitRebuildTestSupport}, never
   * compared against a hardcoded or pre-retry Git SHA/timestamp.
   */
  private void assertBindingRebuiltCorrectly(Connection connection, int notebookId)
      throws Exception {
    BindingSnapshot binding = readBinding(connection, notebookId);
    assertThat(binding.acceptedGitObjectId(), notNullValue());

    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId headObjectId = GitBundleTestReader.fetchHead(readBack, binding.bundleBytes());
      assertThat(
          "recorded head matches the bundle's actual advertised head",
          headObjectId.getName(),
          equalTo(binding.acceptedGitObjectId()));
      try (RevWalk revWalk = new RevWalk(readBack)) {
        RevCommit commit = revWalk.parseCommit(headObjectId);
        assertThat(
            "rebuilt baseline has exactly one parentless root commit",
            commit.getParentCount(),
            equalTo(0));
        List<PortableTreeEntry> actual = readTreeEntries(readBack, commit);

        // suppressClose=true: this adapter is never closed itself, only used to hand the fixture's
        // own raw connection to JdbcTemplate, matching NotebookUpgradeDataPreservationTest's
        // pattern.
        SingleConnectionDataSource singleConnectionDataSource =
            new SingleConnectionDataSource(connection, true);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(singleConnectionDataSource);
        List<PortableTreeEntry> expected =
            currentPortableTreeFromDb(jdbcTemplate, notebookId).stream()
                .sorted((a, b) -> a.path().compareTo(b.path()))
                .toList();
        assertThat(
            "rebuilt tree matches current database content",
            actual,
            contains(expected.toArray(new PortableTreeEntry[0])));
      }
    }
  }
}
