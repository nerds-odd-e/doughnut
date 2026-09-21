package db.migration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.services.notebookGit.NotebookGitBundleImporter;
import com.odde.donut.services.notebookGit.NotebookGitBundleImporter.ImportedBundle;
import com.odde.donut.services.notebookGit.NotebookGitBundleWriter;
import com.odde.donut.services.notebookGit.NotebookGitJdbcFixture;
import com.odde.donut.services.notebookGit.objectstore.JdbcNotebookGitRepository;
import db.migration.NotebookGitUpgradeRehearsalEnvironment.Binding;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.ObjectWalk;
import org.eclipse.jgit.revwalk.RevObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Temporary upgrade rehearsal for retiring {@code bundle_bytes}: the real {@link
 * NotebookGitAcceptedObjectBackfill} over a whole populated {@link
 * NotebookGitUpgradeRehearsalEnvironment} on this worktree's isolated MySQL, followed by an
 * independent reopen/download of every binding and {@link NotebookGitAcceptedHistoryCompleteness},
 * and the expand migration that lets a binding exist without the retained bundle. Removed with the
 * rest of the upgrade machinery.
 */
class NotebookGitUpgradeRehearsalTest {

  private final NotebookGitJdbcFixture jdbc = new NotebookGitJdbcFixture();
  private NotebookGitUpgradeRehearsalEnvironment environment;

  @BeforeEach
  void seedEnvironment() throws Exception {
    environment = new NotebookGitUpgradeRehearsalEnvironment(jdbc);
  }

  @AfterEach
  void removeEnvironment() throws SQLException {
    jdbc.close();
  }

  @Test
  void backfillPreservesEveryHistoryAndLeavesNativeBindingsUntouched() throws Exception {
    Binding staleNative = environment.staleNative;
    Binding partial = environment.partial;
    ObjectId partialMissingBlob = environment.partialMissingBlob;
    Map<Integer, List<Object>> rowsBefore = environment.bindingRows();
    Map<String, String> staleNativeObjectsBefore = environment.nativeObjects(staleNative.id());
    Map<String, String> partialObjectsBefore = environment.nativeObjects(partial.id());

    try (Connection connection = jdbc.openConnection()) {
      NotebookGitAcceptedObjectBackfill.backfillLegacyBindings(connection);
    }

    assertThat("binding rows are untouched", environment.bindingRows(), equalTo(rowsBefore));
    assertThat(environment.nativeObjects(staleNative.id()), equalTo(staleNativeObjectsBefore));
    assertThat(environment.nativeObjects(partial.id()), equalTo(partialObjectsBefore));
    for (Binding binding : environment.allBindings()) {
      if (binding != partial) {
        assertThat(missingObject(binding), equalTo(Optional.empty()));
        assertReopensAndDownloadsExactHistory(binding);
      }
    }
    assertThat(missingObject(partial), equalTo(Optional.of(partialMissingBlob)));
    try (Connection connection = jdbc.openConnection()) {
      IllegalStateException refusal =
          assertThrows(
              IllegalStateException.class,
              () ->
                  NotebookGitAcceptedHistoryCompleteness.requireEveryAcceptedHistoryComplete(
                      connection));
      assertThat(
          refusal.getMessage(),
          containsString("binding " + partial.id() + " is missing " + partialMissingBlob.name()));
    }
  }

  @Test
  void relaxingTheRetainedBundleColumnKeepsEveryRowAndAdmitsBindingsWithoutOne() throws Exception {
    try (Connection connection = jdbc.openConnection();
        Statement statement = connection.createStatement()) {
      statement.execute("ALTER TABLE notebook_git_binding MODIFY bundle_bytes longblob NOT NULL");
      Map<Integer, List<Object>> rowsBefore = environment.bindingRows();

      statement.execute(
          migrationSql("V300000337__allow_notebook_git_binding_without_bundle_bytes"));

      assertThat("binding rows are untouched", environment.bindingRows(), equalTo(rowsBefore));
    }
    int withoutBundle = jdbc.insertBinding(environment.legacy.getFirst().head().name(), null);
    try (Connection connection = jdbc.openConnection();
        PreparedStatement select =
            connection.prepareStatement(
                "SELECT bundle_bytes IS NULL FROM notebook_git_binding WHERE id = ?")) {
      select.setInt(1, withoutBundle);
      try (ResultSet result = select.executeQuery()) {
        result.next();
        assertThat(result.getBoolean(1), is(true));
      }
    }
  }

  @Test
  void anInterruptedBackfillLeavesEachBindingWhollyOldOrWhollyNativeAndARerunCompletesIt()
      throws Exception {
    try (Connection connection = jdbc.openConnection()) {
      assertThrows(
          SQLException.class,
          () ->
              NotebookGitAcceptedObjectBackfill.backfillLegacyBindings(
                  crashingBeforeSecondCommit(connection)));
    }

    List<Boolean> stillOld = new ArrayList<>();
    for (Binding binding : environment.legacy) {
      boolean old = environment.nativeObjects(binding.id()).isEmpty();
      stillOld.add(old);
      if (!old) {
        assertThat(missingObject(binding), equalTo(Optional.empty()));
      }
    }
    assertThat("the interruption left a binding unconverted", stillOld, hasItem(true));

    try (Connection connection = jdbc.openConnection()) {
      NotebookGitAcceptedObjectBackfill.backfillLegacyBindings(connection);
    }
    for (Binding binding : environment.legacy) {
      assertReopensAndDownloadsExactHistory(binding);
    }
  }

  private void assertReopensAndDownloadsExactHistory(Binding binding) throws Exception {
    try (Connection connection = jdbc.openConnection();
        JdbcNotebookGitRepository reopened =
            new JdbcNotebookGitRepository(binding.id(), connection)) {
      assertThat(reopened.exactRef("refs/heads/main").getObjectId(), equalTo(binding.head()));
      for (ObjectId id : reachable(binding.source(), binding.head())) {
        ObjectLoader expected = binding.source().open(id);
        ObjectLoader actual = reopened.open(id);
        assertThat(actual.getType(), equalTo(expected.getType()));
        assertThat(actual.getBytes(), equalTo(expected.getBytes()));
      }
      byte[] download = NotebookGitBundleWriter.write(reopened).bundleBytes();
      try (ImportedBundle clone =
          NotebookGitBundleImporter.importAndVerifyMainHead(download, "download", binding.head())) {
        assertThat(
            reachable(clone.repository(), binding.head()),
            equalTo(reachable(binding.source(), binding.head())));
      }
    }
  }

  private static String migrationSql(String name) throws Exception {
    try (InputStream sql =
        NotebookGitUpgradeRehearsalTest.class.getResourceAsStream(
            "/db/migration/" + name + ".sql")) {
      return new String(sql.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private Optional<ObjectId> missingObject(Binding binding) throws Exception {
    try (Connection connection = jdbc.openConnection()) {
      return NotebookGitAcceptedHistoryCompleteness.firstMissingReachableObject(
          connection, binding.id());
    }
  }

  private static Connection crashingBeforeSecondCommit(Connection real) {
    AtomicInteger commits = new AtomicInteger();
    return (Connection)
        Proxy.newProxyInstance(
            Connection.class.getClassLoader(),
            new Class<?>[] {Connection.class},
            (proxy, method, args) -> {
              if (method.getName().equals("commit") && commits.incrementAndGet() == 2) {
                real.close();
                throw new SQLException("simulated crash before commit");
              }
              try {
                return method.invoke(real, args);
              } catch (InvocationTargetException e) {
                throw e.getCause();
              }
            });
  }

  private static List<ObjectId> reachable(Repository repository, ObjectId head) throws Exception {
    List<ObjectId> ids = new ArrayList<>();
    try (ObjectWalk walk = new ObjectWalk(repository)) {
      walk.markStart(walk.parseCommit(head));
      for (RevObject o = walk.next(); o != null; o = walk.next()) {
        ids.add(o.copy());
      }
      for (RevObject o = walk.nextObject(); o != null; o = walk.nextObject()) {
        ids.add(o.copy());
      }
    }
    ids.sort(ObjectId::compareTo);
    return ids;
  }
}
