package db.migration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.odde.donut.services.notebookExport.PortableTreeEntry;
import com.odde.donut.services.notebookGit.NotebookGitBundleBuilder;
import com.odde.donut.services.notebookGit.NotebookGitBundleWriter;
import com.odde.donut.services.notebookGit.NotebookGitJdbcFixture;
import com.odde.donut.testability.NotebookGitAcceptedHistoryFixture;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;

/**
 * Temporary pre-upgrade environment for {@link NotebookGitUpgradeRehearsalTest}: untouched legacy
 * bindings, an already-native binding whose retained bundle is stale, and a partially native
 * binding missing one reachable blob - each with several history commits - plus snapshots of their
 * persisted rows. Removed with the rest of the upgrade machinery.
 */
final class NotebookGitUpgradeRehearsalEnvironment {

  record Binding(int id, Repository source, ObjectId head) {}

  private final NotebookGitJdbcFixture jdbc;
  final List<Binding> legacy = new ArrayList<>();
  final Binding staleNative;
  final Binding partial;
  final ObjectId partialMissingBlob;

  NotebookGitUpgradeRehearsalEnvironment(NotebookGitJdbcFixture jdbc) throws Exception {
    this.jdbc = jdbc;
    for (String label : List.of("alpha", "beta", "gamma")) {
      Repository source = history(label, 3);
      ObjectId head = mainHead(source);
      legacy.add(new Binding(jdbc.insertBinding(head.name(), bundle(source)), source, head));
    }
    Repository staleSource = history("stale", 1);
    byte[] staleBundle = bundle(staleSource);
    appendCommits(staleSource, "stale", 1, 3);
    staleNative = seedNative(staleSource, staleBundle);
    Repository partialSource = history("partial", 2);
    partial = seedNative(partialSource, bundle(partialSource));
    partialMissingBlob = partialSource.resolve(partial.head().name() + ":partial.md");
    try (Connection connection = jdbc.openConnection();
        PreparedStatement delete =
            connection.prepareStatement(
                "DELETE FROM notebook_git_accepted_object"
                    + " WHERE notebook_git_binding_id = ? AND git_object_id = ?")) {
      delete.setInt(1, partial.id());
      delete.setString(2, partialMissingBlob.name());
      assertThat(delete.executeUpdate(), is(1));
    }
  }

  List<Binding> allBindings() {
    List<Binding> all = new ArrayList<>(legacy);
    all.add(staleNative);
    all.add(partial);
    return all;
  }

  Map<Integer, List<Object>> bindingRows() throws Exception {
    Map<Integer, List<Object>> rows = new TreeMap<>();
    try (Connection connection = jdbc.openConnection();
        PreparedStatement select =
            connection.prepareStatement(
                "SELECT notebook_id, accepted_git_object_id, bundle_bytes, created_at, updated_at"
                    + " FROM notebook_git_binding WHERE id = ?")) {
      for (Binding binding : allBindings()) {
        select.setInt(1, binding.id());
        try (ResultSet result = select.executeQuery()) {
          result.next();
          rows.put(
              binding.id(),
              List.of(
                  result.getInt(1),
                  result.getString(2),
                  sha256(result.getBytes(3)),
                  result.getTimestamp(4),
                  result.getTimestamp(5)));
        }
      }
    }
    return rows;
  }

  Map<String, String> nativeObjects(int bindingId) throws Exception {
    Map<String, String> objects = new TreeMap<>();
    try (Connection connection = jdbc.openConnection();
        PreparedStatement select =
            connection.prepareStatement(
                "SELECT git_object_id, object_type, object_bytes FROM notebook_git_accepted_object"
                    + " WHERE notebook_git_binding_id = ?")) {
      select.setInt(1, bindingId);
      try (ResultSet result = select.executeQuery()) {
        while (result.next()) {
          objects.put(result.getString(1), result.getInt(2) + ":" + sha256(result.getBytes(3)));
        }
      }
    }
    return objects;
  }

  private Binding seedNative(Repository source, byte[] retainedBundle) throws Exception {
    ObjectId head = mainHead(source);
    int id = jdbc.insertBinding(head.name(), retainedBundle);
    jdbc.seedStore(id, source, head);
    return new Binding(id, source, head);
  }

  private static Repository history(String label, int commits) {
    Repository source =
        NotebookGitBundleBuilder.build(
            entries(label, 1), "Donut", "system@donut.local", "c1", time(1));
    appendCommits(source, label, 1, commits);
    return source;
  }

  private static void appendCommits(Repository source, String label, int from, int to) {
    for (int i = from + 1; i <= to; i++) {
      NotebookGitBundleBuilder.append(
          source,
          mainHead(source),
          entries(label, i - 1),
          entries(label, i),
          "Donut",
          "system@donut.local",
          "c" + i,
          time(i));
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

  private static ObjectId mainHead(Repository source) {
    return NotebookGitAcceptedHistoryFixture.mainHeadOf(source);
  }

  private static byte[] bundle(Repository source) {
    return NotebookGitBundleWriter.write(source).bundleBytes();
  }

  private static String sha256(byte[] bytes) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
  }
}
