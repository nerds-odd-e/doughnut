package com.odde.donut.services.notebookGit;

import com.odde.donut.services.notebookGit.objectstore.JdbcNotebookGitRepository;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.Repository;

/**
 * Opens raw JDBC connections to this worktree's test database and creates disposable {@code
 * notebook_git_binding} rows, for tests that exercise a Git object-store adapter directly against
 * real MySQL with no Spring context. Tracks every connection and binding row it hands out so {@link
 * #close()} can release them all.
 */
public final class NotebookGitJdbcFixture implements AutoCloseable {

  private final List<Connection> openConnections = new ArrayList<>();
  private final List<Integer> insertedBindingIds = new ArrayList<>();

  public Connection openConnection() throws SQLException {
    String url = System.getenv("SPRING_DATASOURCE_URL");
    if (url == null || url.isBlank()) {
      url =
          "jdbc:mysql://127.0.0.1:3309/doughnut_test"
              + "?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true";
    }
    Connection connection = DriverManager.getConnection(url, "doughnut", "doughnut");
    openConnections.add(connection);
    return connection;
  }

  public int insertBinding(String initialHeadObjectId) throws SQLException {
    int notebookId = ThreadLocalRandom.current().nextInt(1_500_000_000, 2_000_000_000);
    try (Connection connection = openConnection()) {
      try (Statement pragma = connection.createStatement()) {
        pragma.execute("SET FOREIGN_KEY_CHECKS=0");
      }
      String sql =
          "INSERT INTO notebook_git_binding "
              + "(notebook_id, accepted_git_object_id, created_at, updated_at) "
              + "VALUES (?, ?, NOW(), NOW())";
      try (PreparedStatement statement =
          connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
        statement.setInt(1, notebookId);
        statement.setString(2, initialHeadObjectId);
        statement.executeUpdate();
        try (ResultSet keys = statement.getGeneratedKeys()) {
          keys.next();
          int bindingId = keys.getInt(1);
          insertedBindingIds.add(bindingId);
          return bindingId;
        }
      } finally {
        try (Statement pragma = connection.createStatement()) {
          pragma.execute("SET FOREIGN_KEY_CHECKS=1");
        }
      }
    }
  }

  /** Seeds a fresh store for {@code bindingId} with every object reachable from {@code head}. */
  public void seedStore(int bindingId, Repository source, AnyObjectId head)
      throws IOException, SQLException {
    try (Connection connection = openConnection();
        JdbcNotebookGitRepository store = new JdbcNotebookGitRepository(bindingId, connection)) {
      try (ObjectInserter inserter = store.newObjectInserter()) {
        NotebookGitReachableObjectCopier.copyAllReachableObjects(source, head, inserter);
        inserter.flush();
      }
    }
  }

  @Override
  public void close() throws SQLException {
    try (Connection cleanup = openConnection()) {
      for (int bindingId : insertedBindingIds) {
        try (PreparedStatement statement =
            cleanup.prepareStatement("DELETE FROM notebook_git_binding WHERE id = ?")) {
          statement.setInt(1, bindingId);
          statement.executeUpdate();
        }
      }
    }
    for (Connection connection : openConnections) {
      if (!connection.isClosed()) {
        connection.close();
      }
    }
  }
}
