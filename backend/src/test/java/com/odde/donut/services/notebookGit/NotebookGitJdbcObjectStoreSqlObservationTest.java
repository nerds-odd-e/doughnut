package com.odde.donut.services.notebookGit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

import com.odde.donut.services.notebookGit.objectstore.JdbcNotebookGitRepository;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Calibrates {@link SqlStatementCallLog} against known JDBC store SELECT/INSERT/UPDATE calls and
 * proves batched existence checks for a multi-tree append.
 */
class NotebookGitJdbcObjectStoreSqlObservationTest {

  private final NotebookGitJdbcFixture jdbcFixture = new NotebookGitJdbcFixture();

  @AfterEach
  void closeFixture() throws SQLException {
    jdbcFixture.close();
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
      assertThat(
          "existence check is executed once (JDBC executeQuery, not a wire round-trip count)",
          callLog.countExecutionsMatching("notebook_git_accepted_object", " IN ("),
          equalTo(1L));
      assertThat(
          "missing objects are inserted with one batched INSERT executeUpdate",
          callLog.countExecutionsMatching("INSERT INTO notebook_git_accepted_object"),
          equalTo(1L));
      assertThat(
          "head advances with one conditional UPDATE executeUpdate",
          callLog.countExecutionsMatching("UPDATE notebook_git_binding", "accepted_git_object_id"),
          equalTo(1L));
    }
  }

  @Test
  void recorderCapturesSelectInsertAndUpdateExecutionsAgainstKnownStoreCalls() throws Exception {
    Instant time = Instant.parse("2026-09-20T13:00:00Z");
    List<PortableTreeEntry> v1 = List.of(PortableTreeEntry.ofText("Note.md", "one"));
    List<PortableTreeEntry> v2 = List.of(PortableTreeEntry.ofText("Note.md", "two"));
    try (Repository fixture =
        NotebookGitCommitBuilder.build(
            NotebookGitTreeContent.of(v1), "Donut", "system@donut.local", "Initial", time)) {
      ObjectId baseHead = fixture.exactRef("refs/heads/main").getObjectId();
      int bindingId = jdbcFixture.insertBinding(baseHead.name());
      jdbcFixture.seedStore(bindingId, fixture, baseHead);

      SqlStatementCallLog callLog = new SqlStatementCallLog();
      Connection connection = callLog.wrap(jdbcFixture.openConnection());
      try (JdbcNotebookGitRepository repo = new JdbcNotebookGitRepository(bindingId, connection)) {
        assertThat(repo.open(baseHead), notNullValue());
        commitOn(repo, baseHead, v2, "Edit", time.plusSeconds(1));
      }

      assertThat(
          callLog.countExecutionsMatching(
              "SELECT object_type, object_bytes FROM notebook_git_accepted_object"),
          equalTo(callLog.countObjectFetches()));
      assertThat(callLog.countObjectFetches(), greaterThan(0L));
      assertThat(
          callLog.countExecutionsMatching("INSERT INTO notebook_git_accepted_object"), equalTo(1L));
      assertThat(
          callLog.countExecutionsMatching("UPDATE notebook_git_binding", "accepted_git_object_id"),
          equalTo(1L));
      assertThat(
          callLog.executions().stream()
              .filter(e -> e.sql().contains("INSERT INTO notebook_git_accepted_object"))
              .findFirst()
              .orElseThrow()
              .result(),
          greaterThan(0));
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
}
