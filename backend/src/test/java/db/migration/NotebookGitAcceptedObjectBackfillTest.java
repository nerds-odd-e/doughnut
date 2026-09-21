package db.migration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import com.odde.donut.services.notebookExport.PortableTreeEntry;
import com.odde.donut.services.notebookGit.NotebookGitBundleBuilder;
import com.odde.donut.services.notebookGit.NotebookGitBundleWriter;
import com.odde.donut.services.notebookGit.NotebookGitJdbcFixture;
import com.odde.donut.services.notebookGit.objectstore.JdbcNotebookGitRepository;
import com.odde.donut.testability.GitBundleTestReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Direct, focused test of {@link NotebookGitAcceptedObjectBackfill} - the algorithm behind {@link
 * V300000336__BackfillNotebookGitAcceptedObjects} - against real JGit and this worktree's real
 * isolated MySQL, with no Spring context. This is necessary because the ordinary backend suite only
 * ever runs this Flyway migration against an empty, already-migrated schema (no {@code
 * notebook_git_binding} rows exist yet at schema-migration time in any real environment either), so
 * simply adding the migration file proves nothing about its actual backfill logic: this test
 * manually seeds a genuinely pre-upgrade binding row (bundle bytes present, zero native object
 * rows) and exercises the backfill method directly.
 */
class NotebookGitAcceptedObjectBackfillTest {

  private final NotebookGitJdbcFixture jdbc = new NotebookGitJdbcFixture();

  @AfterEach
  void cleanUp() throws SQLException {
    jdbc.close();
  }

  @Test
  void backfillsALegacyBindingIdempotentlyWithoutTouchingBundleColumns() throws Exception {
    Instant time = Instant.parse("2026-09-20T09:00:00Z");
    List<PortableTreeEntry> v1 = List.of(PortableTreeEntry.ofText("Note.md", "Original body"));
    List<PortableTreeEntry> v2 = List.of(PortableTreeEntry.ofText("Note.md", "Second body"));

    ObjectId commit1;
    ObjectId commit2;
    byte[] bundleBytes;
    try (Repository fixture =
        NotebookGitBundleBuilder.build(v1, "Donut", "system@donut.local", "Initial", time)) {
      commit1 = fixture.exactRef("refs/heads/main").getObjectId();
      commit2 =
          NotebookGitBundleBuilder.append(
              fixture, commit1, v1, v2, "Donut", "system@donut.local", "Edit", time.plusSeconds(1));
      bundleBytes = NotebookGitBundleWriter.write(fixture);
    }

    int seededBindingId = jdbc.insertBinding(commit2.name(), bundleBytes);

    assertThat(
        "seeded binding starts legacy (no native rows)", isLegacy(seededBindingId), is(true));

    try (Connection connection = jdbc.openConnection()) {
      int convertedCount = NotebookGitAcceptedObjectBackfill.backfillLegacyBindings(connection);
      assertThat("at least our seeded binding was converted", convertedCount, greaterThan(0));
    }

    assertThat("binding is no longer legacy after backfill", isLegacy(seededBindingId), is(false));

    // bundle_bytes / accepted_git_object_id are untouched by the backfill.
    try (Connection connection = jdbc.openConnection();
        PreparedStatement statement =
            connection.prepareStatement(
                "SELECT bundle_bytes, accepted_git_object_id FROM notebook_git_binding"
                    + " WHERE id = ?")) {
      statement.setInt(1, seededBindingId);
      try (ResultSet result = statement.executeQuery()) {
        assertThat(result.next(), is(true));
        assertThat(result.getBytes("bundle_bytes"), equalTo(bundleBytes));
        assertThat(result.getString("accepted_git_object_id"), equalTo(commit2.name()));
      }
    }

    long nativeRowCountAfterFirstRun = nativeObjectRowCount(seededBindingId);
    assertThat(nativeRowCountAfterFirstRun, greaterThan(0L));

    // A fresh JdbcNotebookGitRepository on a brand-new connection - no process-local cache - reads
    // back the exact head, parent graph and content the original bundle encoded.
    try (Connection reopenConnection = jdbc.openConnection();
        JdbcNotebookGitRepository reopened =
            new JdbcNotebookGitRepository(seededBindingId, reopenConnection)) {
      assertThat(reopened.exactRef("refs/heads/main").getObjectId(), equalTo(commit2));
      try (RevWalk walk = new RevWalk(reopened)) {
        RevCommit head = walk.parseCommit(commit2);
        assertThat(head.getParent(0).getId(), equalTo(commit1));
        assertThat(
            GitBundleTestReader.readTreeEntries(reopened, head),
            equalTo(List.of(PortableTreeEntry.ofText("Note.md", "Second body"))));
      }
    }

    // Running the backfill a second time on the same, now-converted data is a safe no-op: the
    // binding no longer matches the "still empty native store" query, so it is skipped entirely.
    try (Connection connection = jdbc.openConnection()) {
      NotebookGitAcceptedObjectBackfill.backfillLegacyBindings(connection);
    }
    assertThat("second run leaves the binding non-legacy", isLegacy(seededBindingId), is(false));
    assertThat(
        "second run adds no further native rows for an already-converted binding",
        nativeObjectRowCount(seededBindingId),
        equalTo(nativeRowCountAfterFirstRun));

    // Still readable and correct after the idempotent second run.
    try (Connection reopenConnection = jdbc.openConnection();
        JdbcNotebookGitRepository reopenedAgain =
            new JdbcNotebookGitRepository(seededBindingId, reopenConnection)) {
      assertThat(reopenedAgain.exactRef("refs/heads/main").getObjectId(), equalTo(commit2));
    }
  }

  private boolean isLegacy(int bindingId) throws SQLException {
    try (Connection connection = jdbc.openConnection();
        PreparedStatement statement =
            connection.prepareStatement(
                "SELECT 1 FROM notebook_git_binding b WHERE b.id = ? AND NOT EXISTS ("
                    + "SELECT 1 FROM notebook_git_accepted_object o"
                    + " WHERE o.notebook_git_binding_id = b.id)")) {
      statement.setInt(1, bindingId);
      try (ResultSet result = statement.executeQuery()) {
        return result.next();
      }
    }
  }

  private long nativeObjectRowCount(int bindingId) throws SQLException {
    try (Connection connection = jdbc.openConnection();
        PreparedStatement statement =
            connection.prepareStatement(
                "SELECT COUNT(*) FROM notebook_git_accepted_object WHERE notebook_git_binding_id"
                    + " = ?")) {
      statement.setInt(1, bindingId);
      try (ResultSet result = statement.executeQuery()) {
        result.next();
        return result.getLong(1);
      }
    }
  }
}
