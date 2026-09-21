package db.migration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.services.notebookExport.PortableTreeEntry;
import com.odde.donut.services.notebookGit.NotebookGitCommitBuilder;
import com.odde.donut.services.notebookGit.NotebookGitJdbcFixture;
import com.odde.donut.testability.NotebookGitAcceptedHistoryFixture;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * {@link NotebookGitAcceptedHistoryCompleteness} - the gate the column-dropping migration runs
 * before its DDL - against real JGit and this worktree's isolated MySQL, with no Spring context.
 */
class NotebookGitAcceptedHistoryCompletenessTest {

  private final NotebookGitJdbcFixture jdbc = new NotebookGitJdbcFixture();

  @AfterEach
  void cleanUp() throws SQLException {
    jdbc.close();
  }

  @Test
  void everyAcceptedHistoryHeldWhollyInNativeStorageIsComplete() throws Exception {
    seedCompleteBinding("complete");

    try (Connection connection = jdbc.openConnection()) {
      NotebookGitAcceptedHistoryCompleteness.requireEveryAcceptedHistoryComplete(connection);
    }
  }

  @ParameterizedTest(name = "missing {0}")
  @CsvSource({"head commit, ''", "tree, ':shared'", "blob, ':shared/unchanged.md'"})
  void aBindingMissingAReachableObjectBlocksTheDrop(String kind, String pathFromHead)
      throws Exception {
    seedCompleteBinding("complete");
    Binding incomplete = seedCompleteBinding("incomplete");
    ObjectId missing = incomplete.source().resolve(incomplete.head().name() + pathFromHead);
    removeStoredObject(incomplete.id(), missing);

    try (Connection connection = jdbc.openConnection()) {
      IllegalStateException refusal =
          assertThrows(
              IllegalStateException.class,
              () ->
                  NotebookGitAcceptedHistoryCompleteness.requireEveryAcceptedHistoryComplete(
                      connection));
      assertThat(
          refusal.getMessage(),
          equalTo(
              "Native accepted history is incomplete; bundle_bytes must not be dropped:"
                  + " binding "
                  + incomplete.id()
                  + " is missing "
                  + missing.name()));
    }
  }

  private record Binding(int id, Repository source, ObjectId head) {}

  private Binding seedCompleteBinding(String label) throws Exception {
    Repository source =
        NotebookGitCommitBuilder.build(
            entries(label, 1), "Donut", "system@donut.local", "c1", time(1));
    ObjectId head =
        NotebookGitCommitBuilder.append(
            source,
            NotebookGitAcceptedHistoryFixture.mainHeadOf(source),
            entries(label, 2),
            "Donut",
            "system@donut.local",
            "c2",
            time(2));
    int bindingId = jdbc.insertBinding(head.name());
    jdbc.seedStore(bindingId, source, head);
    return new Binding(bindingId, source, head);
  }

  private void removeStoredObject(int bindingId, ObjectId missing) throws SQLException {
    try (Connection connection = jdbc.openConnection();
        PreparedStatement delete =
            connection.prepareStatement(
                "DELETE FROM notebook_git_accepted_object"
                    + " WHERE notebook_git_binding_id = ? AND git_object_id = ?")) {
      delete.setInt(1, bindingId);
      delete.setString(2, missing.name());
      assertThat(delete.executeUpdate(), is(1));
    }
  }

  private static List<PortableTreeEntry> entries(String label, int revision) {
    return List.of(
        PortableTreeEntry.ofText(label + ".md", label + " revision " + revision),
        PortableTreeEntry.ofText("shared/unchanged.md", "unchanged " + label));
  }

  private static Instant time(int revision) {
    return Instant.parse("2026-09-21T09:00:00Z").plusSeconds(revision);
  }
}
