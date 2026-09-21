package db.migration;

import com.odde.donut.services.notebookGit.NotebookGitBundleImporter;
import com.odde.donut.services.notebookGit.NotebookGitBundleImporter.ImportedBundle;
import com.odde.donut.services.notebookGit.NotebookGitReachableObjectCopier;
import com.odde.donut.services.notebookGit.objectstore.JdbcNotebookGitRepository;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.Repository;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * One-time upgrade step: converts every {@code notebook_git_binding} row whose native object store
 * ({@code notebook_git_accepted_object}) is still empty into native storage: import the binding's
 * bundle bytes, verify the imported {@code main} head matches the binding's persisted accepted
 * head, then copy every reachable object into the native store, keeping its exact head and history.
 * {@code bundle_bytes} and {@code accepted_git_object_id} are only read, never modified; this
 * migration only adds native rows. A fresh install runs it over empty tables, where it does
 * nothing.
 *
 * <p>Each binding is imported, verified, copied and committed as its own unit of work ({@link
 * #canExecuteInTransaction()} is {@code false}), so an interrupted run (crash, restart) leaves
 * every binding wholly old (zero native rows) or wholly native (every reachable object committed).
 * An already-converted binding no longer matches the "still empty native store" query and is
 * skipped, so a rerun after an interruption resumes safely with no separate checkpoint bookkeeping.
 */
public class V300000336__BackfillNotebookGitAcceptedObjects extends BaseJavaMigration {

  @Override
  public boolean canExecuteInTransaction() {
    return false;
  }

  @Override
  public void migrate(Context context) throws SQLException, IOException {
    Connection connection = context.getConnection();
    connection.setAutoCommit(false);
    for (int bindingId : legacyBindingIds(connection)) {
      backfillOne(connection, bindingId);
    }
  }

  private static void backfillOne(Connection connection, int bindingId)
      throws SQLException, IOException {
    LegacyBinding legacy = readLegacyBinding(connection, bindingId);
    try (ImportedBundle imported =
        NotebookGitBundleImporter.importAndVerifyMainHead(
            legacy.bundleBytes(), "backfill-binding-" + bindingId, legacy.acceptedHead())) {
      Repository nativeRepository = new JdbcNotebookGitRepository(bindingId, connection);
      try (ObjectInserter inserter = nativeRepository.newObjectInserter()) {
        NotebookGitReachableObjectCopier.copyAllReachableObjects(
            imported.repository(), imported.mainHead(), inserter);
        inserter.flush();
      }
    } catch (IOException | RuntimeException e) {
      connection.rollback();
      throw e;
    }
    connection.commit();
  }

  private static List<Integer> legacyBindingIds(Connection connection) throws SQLException {
    String sql =
        """
        SELECT b.id FROM notebook_git_binding b
        WHERE NOT EXISTS (
          SELECT 1 FROM notebook_git_accepted_object o
          WHERE o.notebook_git_binding_id = b.id
        )
        """;
    try (Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery(sql)) {
      List<Integer> ids = new ArrayList<>();
      while (result.next()) {
        ids.add(result.getInt("id"));
      }
      return ids;
    }
  }

  private static LegacyBinding readLegacyBinding(Connection connection, int bindingId)
      throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement(
            "SELECT bundle_bytes, accepted_git_object_id FROM notebook_git_binding WHERE id = ?")) {
      statement.setInt(1, bindingId);
      try (ResultSet result = statement.executeQuery()) {
        if (!result.next()) {
          throw new SQLException("Legacy binding " + bindingId + " no longer exists");
        }
        return new LegacyBinding(
            result.getBytes("bundle_bytes"),
            ObjectId.fromString(result.getString("accepted_git_object_id")));
      }
    }
  }

  private record LegacyBinding(byte[] bundleBytes, ObjectId acceptedHead) {}
}
