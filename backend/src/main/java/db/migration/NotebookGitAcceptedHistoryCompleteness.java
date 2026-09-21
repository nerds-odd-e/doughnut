package db.migration;

import com.odde.donut.services.notebookGit.objectstore.JdbcNotebookGitRepository;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.ObjectWalk;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;

/**
 * Verifies the end state native storage must reach before the retained {@code bundle_bytes} column
 * may be dropped: every {@code notebook_git_binding}'s persisted accepted head, and every object it
 * reaches, is present in {@code notebook_git_accepted_object}. A nonempty native store is not
 * evidence - a binding holding only some of its reachable objects is never re-selected by {@link
 * NotebookGitAcceptedObjectBackfill}, whichever writer left it partial - so this walks each head's
 * reachable graph instead of counting rows.
 *
 * <p>A plain class outside Spring context beside the backfill, because the column-dropping Flyway
 * migration must run it before its destructive DDL; it is removed with the rest of the upgrade
 * machinery.
 */
final class NotebookGitAcceptedHistoryCompleteness {

  private NotebookGitAcceptedHistoryCompleteness() {}

  /** Fails loudly, naming each incomplete binding, unless every accepted history is complete. */
  static void requireEveryAcceptedHistoryComplete(Connection connection)
      throws SQLException, IOException {
    List<String> incomplete = new ArrayList<>();
    for (int bindingId : bindingIds(connection)) {
      Optional<ObjectId> missing = firstMissingReachableObject(connection, bindingId);
      if (missing.isPresent()) {
        incomplete.add("binding " + bindingId + " is missing " + missing.get().name());
      }
    }
    if (!incomplete.isEmpty()) {
      throw new IllegalStateException(
          "Native accepted history is incomplete; retained bundles must not be dropped: "
              + String.join(", ", incomplete));
    }
  }

  /** The first object reachable from the binding's accepted head that native storage lacks. */
  private static Optional<ObjectId> firstMissingReachableObject(
      Connection connection, int bindingId) throws SQLException, IOException {
    Set<String> stored = storedObjectIds(connection, bindingId);
    try (JdbcNotebookGitRepository repository =
            new JdbcNotebookGitRepository(bindingId, connection);
        ObjectWalk walk = new ObjectWalk(repository)) {
      walk.markStart(walk.parseCommit(acceptedHead(connection, bindingId)));
      RevCommit commit;
      while ((commit = walk.next()) != null) {
        if (!stored.contains(commit.name())) {
          return Optional.of(commit.copy());
        }
      }
      RevObject object;
      while ((object = walk.nextObject()) != null) {
        if (!stored.contains(object.name())) {
          return Optional.of(object.copy());
        }
      }
      return Optional.empty();
    } catch (MissingObjectException e) {
      return Optional.of(e.getObjectId().copy());
    }
  }

  private static List<Integer> bindingIds(Connection connection) throws SQLException {
    try (Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery("SELECT id FROM notebook_git_binding")) {
      List<Integer> ids = new ArrayList<>();
      while (result.next()) {
        ids.add(result.getInt("id"));
      }
      return ids;
    }
  }

  private static ObjectId acceptedHead(Connection connection, int bindingId) throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement(
            "SELECT accepted_git_object_id FROM notebook_git_binding WHERE id = ?")) {
      statement.setInt(1, bindingId);
      try (ResultSet result = statement.executeQuery()) {
        if (!result.next()) {
          throw new SQLException("Binding " + bindingId + " no longer exists");
        }
        return ObjectId.fromString(result.getString("accepted_git_object_id"));
      }
    }
  }

  private static Set<String> storedObjectIds(Connection connection, int bindingId)
      throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement(
            "SELECT git_object_id FROM notebook_git_accepted_object"
                + " WHERE notebook_git_binding_id = ?")) {
      statement.setInt(1, bindingId);
      try (ResultSet result = statement.executeQuery()) {
        Set<String> ids = new HashSet<>();
        while (result.next()) {
          ids.add(result.getString("git_object_id"));
        }
        return ids;
      }
    }
  }
}
