package com.odde.donut.services.notebookGit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookExport.ExportFolderRow;
import com.odde.donut.services.notebookExport.ExportNoteRow;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import com.odde.donut.services.notebookExport.PortableTreeSnapshot;
import com.odde.donut.testability.GitBundleTestReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;

/**
 * Shared raw-connection helpers for notebook-git baseline rebuild and fleet cutover backfill tests,
 * which commit bindings outside a Spring test transaction. Owns the raw JDBC entry points,
 * committed-row cleanup, bundle read-back, and Portable tree comparison against current database
 * content.
 */
final class NotebookGitRebuildTestSupport {

  private NotebookGitRebuildTestSupport() {}

  static void runBackfill(DataSource dataSource, Instant cutoverTime) throws Exception {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      NotebookGitFleetCutoverBackfill.run(connection, cutoverTime);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  static void runRebuild(DataSource dataSource, int notebookId, Instant rebuildTime)
      throws Exception {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      NotebookGitBaselineRebuild.rebuildNotebook(connection, notebookId, rebuildTime);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  static void deleteOwnedNotebookGraph(JdbcTemplate jdbcTemplate, int ownerUserId) {
    jdbcTemplate.update(
        "DELETE FROM notebook_git_binding WHERE notebook_id IN "
            + "(SELECT id FROM notebook WHERE creator_id = ?)",
        ownerUserId);
    jdbcTemplate.update(
        "DELETE FROM note WHERE notebook_id IN " + "(SELECT id FROM notebook WHERE creator_id = ?)",
        ownerUserId);
    jdbcTemplate.update(
        "DELETE FROM folder WHERE notebook_id IN "
            + "(SELECT id FROM notebook WHERE creator_id = ?)",
        ownerUserId);
    jdbcTemplate.update("DELETE FROM notebook WHERE creator_id = ?", ownerUserId);
    jdbcTemplate.update("DELETE FROM user WHERE id = ?", ownerUserId);
  }

  static void assertBundleTreeEqualsCurrentContent(
      NotebookGitBinding binding, int notebookId, DataSource dataSource, JdbcTemplate jdbcTemplate)
      throws Exception {
    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId headObjectId = GitBundleTestReader.fetchHead(readBack, binding.getBundleBytes());
      assertThat(headObjectId.getName(), equalTo(binding.getAcceptedGitObjectId()));
      try (RevWalk revWalk = new RevWalk(readBack)) {
        RevCommit commit = revWalk.parseCommit(headObjectId);
        assertThat(commit.getParentCount(), equalTo(0));
        List<PortableTreeEntry> foundEntries = readTreeEntries(readBack, commit);
        List<PortableTreeEntry> expectedEntries =
            currentPortableTreeFromDb(jdbcTemplate, notebookId);
        List<PortableTreeEntry> sortedExpected =
            expectedEntries.stream().sorted((a, b) -> a.path().compareTo(b.path())).toList();
        assertThat(foundEntries, contains(sortedExpected.toArray(new PortableTreeEntry[0])));
      }
    }
  }

  static List<PortableTreeEntry> readTreeEntries(InMemoryRepository repository, RevCommit commit)
      throws Exception {
    List<PortableTreeEntry> entries = new ArrayList<>();
    try (TreeWalk treeWalk = new TreeWalk(repository)) {
      treeWalk.addTree(commit.getTree());
      treeWalk.setRecursive(true);
      while (treeWalk.next()) {
        ObjectId blobId = treeWalk.getObjectId(0);
        ObjectLoader loader = repository.open(blobId);
        String content = new String(loader.getBytes(), StandardCharsets.UTF_8);
        entries.add(new PortableTreeEntry(treeWalk.getPathString(), content));
      }
    }
    return entries.stream().sorted((a, b) -> a.path().compareTo(b.path())).toList();
  }

  static List<PortableTreeEntry> currentPortableTreeFromDb(
      JdbcTemplate jdbcTemplate, int notebookId) {
    String readme =
        jdbcTemplate.queryForObject(
            "SELECT readme_content FROM notebook WHERE id = ?", String.class, notebookId);
    List<ExportFolderRow> folders =
        jdbcTemplate.query(
            """
            SELECT id, parent_folder_id, name, readme_content
            FROM folder
            WHERE notebook_id = ?
            ORDER BY id ASC
            """,
            (rs, ignored) ->
                new ExportFolderRow(
                    rs.getInt("id"),
                    rs.wasNull() ? null : rs.getInt("parent_folder_id"),
                    rs.getString("name"),
                    rs.getString("readme_content")),
            notebookId);
    List<ExportNoteRow> notes =
        jdbcTemplate.query(
            """
            SELECT folder_id, title, content
            FROM note
            WHERE notebook_id = ?
            ORDER BY id ASC
            """,
            (rs, ignored) ->
                new ExportNoteRow(
                    rs.wasNull() ? null : rs.getInt("folder_id"),
                    rs.getString("title"),
                    rs.getString("content")),
            notebookId);
    return PortableTreeSnapshot.build(readme, folders, notes);
  }
}
