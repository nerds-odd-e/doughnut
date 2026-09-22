package com.odde.donut.services.notebookGit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.services.notebookGit.objectstore.JdbcNotebookGitRepository;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import com.odde.donut.testability.GitBundleTestReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the JDBC-backed native Git object store adapter directly against real JGit and the test
 * MySQL database, with no Spring context.
 */
class NotebookGitJdbcObjectStoreTest {

  private final NotebookGitJdbcFixture jdbcFixture = new NotebookGitJdbcFixture();

  @AfterEach
  void closeFixture() throws SQLException {
    jdbcFixture.close();
  }

  @Test
  void roundTripsThroughCloseReopenAppendAndExport() throws Exception {
    Instant time = Instant.parse("2026-09-20T10:00:00Z");
    List<PortableTreeEntry> v1 =
        List.of(
            PortableTreeEntry.ofText("Parent/README.md", "Parent readme"),
            PortableTreeEntry.ofText("Parent/Child/Note.md", "First body"));
    List<PortableTreeEntry> v2 =
        List.of(
            PortableTreeEntry.ofText("Parent/README.md", "Parent readme"),
            PortableTreeEntry.ofText("Parent/Child/Note.md", "Second body"));
    List<PortableTreeEntry> v3 =
        List.of(
            PortableTreeEntry.ofText("Parent/README.md", "Parent readme"),
            PortableTreeEntry.ofText("Parent/Child/Note.md", "Second body"),
            PortableTreeEntry.ofText("Parent/Added.md", "New note"));

    ObjectId commit1;
    ObjectId commit2;
    ObjectId commit3;
    byte[] fixtureBundleBytes;
    try (Repository fixture =
        NotebookGitCommitBuilder.build(
            NotebookGitTreeContent.of(v1), "Donut", "system@donut.local", "Initial", time)) {
      commit1 = fixture.exactRef("refs/heads/main").getObjectId();
      commit2 = commitOn(fixture, commit1, v2, "Edit", time.plusSeconds(1));
      commit3 = commitOn(fixture, commit2, v3, "Add", time.plusSeconds(2));
      fixtureBundleBytes = NotebookGitBundleWriter.write(fixture);
    }

    int bindingId = jdbcFixture.insertBinding(commit3.name());

    // Import the multi-commit bundle: standard JGit pack-parsing into a scratch repository, then
    // an ObjectWalk copy of every reachable object into this store's ObjectInserter - this store
    // never parses inbound pack streams itself.
    try (NotebookGitBundleImporter.ImportedBundle imported =
            NotebookGitBundleImporter.importMainHead(fixtureBundleBytes, "fixture");
        Connection importConnection = jdbcFixture.openConnection();
        JdbcNotebookGitRepository store =
            new JdbcNotebookGitRepository(bindingId, importConnection)) {
      assertThat(imported.mainHead(), equalTo(commit3));
      try (ObjectInserter inserter = store.newObjectInserter()) {
        NotebookGitReachableObjectCopier.copyAllReachableObjects(
            imported.repository(), imported.mainHead(), inserter);
        inserter.flush();
      }
    }

    // Close, reopen a fresh instance on a fresh connection, and confirm an exact head/tree match -
    // no process-local cache.
    try (Connection reopenConnection = jdbcFixture.openConnection();
        JdbcNotebookGitRepository reopened =
            new JdbcNotebookGitRepository(bindingId, reopenConnection)) {
      assertThat(reopened.exactRef("refs/heads/main").getObjectId(), equalTo(commit3));
      assertTreeEntries(
          reopened,
          commit3,
          List.of(
              PortableTreeEntry.ofText("Parent/Added.md", "New note"),
              PortableTreeEntry.ofText("Parent/Child/Note.md", "Second body"),
              PortableTreeEntry.ofText("Parent/README.md", "Parent readme")));

      // Append one more commit through the adapter itself (its own ObjectInserter + RefUpdate).
      List<PortableTreeEntry> v4 =
          List.of(
              PortableTreeEntry.ofText("Parent/README.md", "Parent readme"),
              PortableTreeEntry.ofText("Parent/Child/Note.md", "Third body"),
              PortableTreeEntry.ofText("Parent/Added.md", "New note"));
      ObjectId commit4 = commitOn(reopened, commit3, v4, "Third edit", time.plusSeconds(3));

      assertThat(reopened.exactRef("refs/heads/main").getObjectId(), equalTo(commit4));

      // Reopen again on yet another fresh connection, export, and confirm a fresh in-memory JGit
      // repository can fetch the result.
      try (Connection exportConnection = jdbcFixture.openConnection();
          JdbcNotebookGitRepository reopenedAgain =
              new JdbcNotebookGitRepository(bindingId, exportConnection)) {
        assertThat(reopenedAgain.exactRef("refs/heads/main").getObjectId(), equalTo(commit4));

        byte[] exportedBundleBytes = NotebookGitBundleWriter.write(reopenedAgain);
        try (InMemoryRepository scratch = new InMemoryRepository(new DfsRepositoryDescription())) {
          ObjectId fetchedHead = GitBundleTestReader.fetchHead(scratch, exportedBundleBytes);
          assertThat(fetchedHead, equalTo(commit4));
          assertTreeEntries(
              scratch,
              fetchedHead,
              List.of(
                  PortableTreeEntry.ofText("Parent/Added.md", "New note"),
                  PortableTreeEntry.ofText("Parent/Child/Note.md", "Third body"),
                  PortableTreeEntry.ofText("Parent/README.md", "Parent readme")));
        }
      }
    }
  }

  @Test
  void abortedTransactionLeavesNoTornStateVisibleFromAnotherConnectionOrAReopenedInstance()
      throws Exception {
    Instant time = Instant.parse("2026-09-20T11:00:00Z");
    List<PortableTreeEntry> v1 = List.of(PortableTreeEntry.ofText("Note.md", "Committed body"));
    List<PortableTreeEntry> v2 = List.of(PortableTreeEntry.ofText("Note.md", "Aborted body"));

    ObjectId commit1;
    try (Repository fixture =
        NotebookGitCommitBuilder.build(
            NotebookGitTreeContent.of(v1), "Donut", "system@donut.local", "Initial", time)) {
      commit1 = fixture.exactRef("refs/heads/main").getObjectId();

      int bindingId = jdbcFixture.insertBinding(commit1.name());
      jdbcFixture.seedStore(bindingId, fixture, commit1);

      Connection txConnection = jdbcFixture.openConnection();
      txConnection.setAutoCommit(false);
      txConnection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
      ObjectId commit2;
      try (JdbcNotebookGitRepository txRepo =
          new JdbcNotebookGitRepository(bindingId, txConnection)) {
        commit2 = commitOn(txRepo, commit1, v2, "Aborted edit", time.plusSeconds(1));

        // Visible on this connection's own uncommitted transaction.
        assertThat(txRepo.exactRef("refs/heads/main").getObjectId(), equalTo(commit2));
        assertThat(txRepo.open(commit2), notNullValue());

        txConnection.rollback();

        // Rolled back: this same connection now sees the old head again.
        assertThat(txRepo.exactRef("refs/heads/main").getObjectId(), equalTo(commit1));
        assertThrows(MissingObjectException.class, () -> txRepo.open(commit2));
      } finally {
        txConnection.close();
      }

      // A separate connection and a freshly reopened instance (no in-process cache) never saw the
      // aborted write either.
      try (Connection otherConnection = jdbcFixture.openConnection();
          JdbcNotebookGitRepository reopened =
              new JdbcNotebookGitRepository(bindingId, otherConnection)) {
        assertThat(reopened.exactRef("refs/heads/main").getObjectId(), equalTo(commit1));
        assertThrows(MissingObjectException.class, () -> reopened.open(commit2));
      }
    }
  }

  @Test
  void oneAppendAcrossManyTreesIssuesOneBatchedExistenceCheckQuery() throws Exception {
    Instant time = Instant.parse("2026-09-20T12:00:00Z");
    List<PortableTreeEntry> baseEntries =
        List.of(
            PortableTreeEntry.ofText("A/B/C/Leaf.md", "before"),
            PortableTreeEntry.ofText("A/B/Sibling.md", "unchanged"),
            PortableTreeEntry.ofText("A/Other/Note.md", "unchanged"),
            PortableTreeEntry.ofText("Top.md", "unchanged"));
    List<PortableTreeEntry> changedEntries =
        List.of(
            PortableTreeEntry.ofText("A/B/C/Leaf.md", "after"),
            PortableTreeEntry.ofText("A/B/Sibling.md", "unchanged"),
            PortableTreeEntry.ofText("A/Other/Note.md", "unchanged"),
            PortableTreeEntry.ofText("Top.md", "unchanged"));

    ObjectId baseHead;
    try (Repository fixture =
        NotebookGitCommitBuilder.build(
            NotebookGitTreeContent.of(baseEntries),
            "Donut",
            "system@donut.local",
            "Initial",
            time)) {
      baseHead = fixture.exactRef("refs/heads/main").getObjectId();

      int bindingId = jdbcFixture.insertBinding(baseHead.name());
      jdbcFixture.seedStore(bindingId, fixture, baseHead);

      SqlStatementCallLog callLog = new SqlStatementCallLog();
      Connection appendConnection = callLog.wrap(jdbcFixture.openConnection());
      ObjectId newHead;
      try (JdbcNotebookGitRepository repo =
          new JdbcNotebookGitRepository(bindingId, appendConnection)) {
        newHead = commitOn(repo, baseHead, changedEntries, "Deep edit", time.plusSeconds(1));
      }

      assertThat(newHead, notNullValue());
      assertThat(
          "one batched existence-check query for the whole flush, not one per attempted tree",
          callLog.countMatching("notebook_git_accepted_object", " IN ("),
          equalTo(1L));
    }
  }

  private static ObjectId commitOn(
      Repository repo,
      ObjectId parent,
      List<PortableTreeEntry> entries,
      String message,
      Instant commitTime) {
    return NotebookGitCommitBuilder.append(
        repo,
        parent,
        NotebookGitTreeContent.of(entries),
        "Donut",
        "system@donut.local",
        message,
        commitTime);
  }

  private void assertTreeEntries(
      Repository repo, ObjectId commitId, List<PortableTreeEntry> expectedEntries)
      throws IOException {
    try (RevWalk walk = new RevWalk(repo)) {
      assertThat(
          GitBundleTestReader.readTreeEntries(repo, walk.parseCommit(commitId)),
          equalTo(expectedEntries));
    }
  }
}
