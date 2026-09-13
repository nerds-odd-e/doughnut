package com.odde.donut.db.migration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import com.odde.donut.services.notebookGit.NotebookGitFleetCutoverBackfill;
import com.odde.donut.testability.GitBundleTestReader;
import com.odde.donut.testability.MakeMe;
import db.migration.V300000326__MigrateLegacyDeletedNotesToTrash;
import db.migration.V300000327__RebuildNotebookGitBaselines;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Timestamp;
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
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exercises the registered Flyway data migrations {@link
 * V300000326__MigrateLegacyDeletedNotesToTrash} and {@link V300000327__RebuildNotebookGitBaselines}
 * against a real connection with no ambient Spring test transaction. Flyway auto-discovers these
 * migrations and runs them once on context startup, before any test fixture exists, so the test
 * sets up legacy state after startup and then invokes the migration classes directly to prove their
 * {@code migrate(Context)} behaviour. Mirrors the raw-connection pattern of {@code
 * NotebookGitBaselineRebuildTest} and cleans up its own committed rows explicitly.
 */
@SpringBootTest
@ActiveProfiles("test")
class RecoverLegacyNotesAndRebuildGitMigrationsTest {

  @Autowired MakeMe makeMe;
  @Autowired DataSource dataSource;
  @Autowired JdbcTemplate jdbcTemplate;
  @Autowired NotebookGitBindingRepository notebookGitBindingRepository;

  private final List<Integer> ownerUserIdsToClean = new ArrayList<>();

  @AfterEach
  void cleanUp() {
    for (Integer ownerUserId : ownerUserIdsToClean) {
      jdbcTemplate.update(
          "DELETE FROM notebook_git_binding WHERE notebook_id IN "
              + "(SELECT id FROM notebook WHERE creator_id = ?)",
          ownerUserId);
      jdbcTemplate.update(
          "DELETE FROM memory_tracker WHERE note_id IN "
              + "(SELECT id FROM note WHERE notebook_id IN "
              + "(SELECT id FROM notebook WHERE creator_id = ?))",
          ownerUserId);
      jdbcTemplate.update(
          "DELETE FROM note WHERE notebook_id IN "
              + "(SELECT id FROM notebook WHERE creator_id = ?)",
          ownerUserId);
      jdbcTemplate.update(
          "DELETE FROM folder WHERE notebook_id IN "
              + "(SELECT id FROM notebook WHERE creator_id = ?)",
          ownerUserId);
      jdbcTemplate.update("DELETE FROM notebook WHERE creator_id = ?", ownerUserId);
      jdbcTemplate.update("DELETE FROM user WHERE id = ?", ownerUserId);
    }
    ownerUserIdsToClean.clear();
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void flywayChainMigratesLegacyDeletedNotesIntoTrashAndRebuildsLiveNotebookBaselines()
      throws Exception {
    User owner = makeMe.aUser().please();
    ownerUserIdsToClean.add(owner.getId());
    Notebook notebook =
        makeMe.aNotebook().creatorAndOwner(owner).readmeContent("# Notebook readme").please();
    Folder recipes = makeMe.aFolder().notebook(notebook).name("Recipes").please();
    Note pasta = makeMe.aNote("Pasta").folder(recipes).content("Boil water").please();
    Note salad = makeMe.aNote("Salad").folder(recipes).content("Toss leaves").please();
    Integer pastaId = pasta.getId();
    Integer notebookId = notebook.getId();

    Instant initialCutover = Instant.parse("2026-09-04T10:15:30Z");
    runBackfill(initialCutover);
    NotebookGitBinding initialBinding =
        notebookGitBindingRepository.findByNotebook_Id(notebookId).orElseThrow();
    String oldAcceptedGitObjectId = initialBinding.getAcceptedGitObjectId();
    byte[] oldBundleBytes = initialBinding.getBundleBytes();

    jdbcTemplate.update(
        "UPDATE note SET deleted_at = ? WHERE id = ?",
        Timestamp.from(Instant.parse("2026-08-01T00:00:00Z")),
        pastaId);

    runTrashMigration();
    runBaselineRebuildMigration();

    assertThat(
        jdbcTemplate.queryForObject(
            "SELECT deleted_at FROM note WHERE id = ?", Timestamp.class, pastaId),
        nullValue());
    Integer migratedFolderId =
        jdbcTemplate.queryForObject(
            "SELECT folder_id FROM note WHERE id = ?", Integer.class, pastaId);
    assertThat(folderPathFromRoot(migratedFolderId), equalTo("_trash/Recipes"));
    assertThat(
        jdbcTemplate.queryForObject("SELECT title FROM note WHERE id = ?", String.class, pastaId),
        equalTo("Pasta"));
    assertThat(
        jdbcTemplate.queryForObject("SELECT content FROM note WHERE id = ?", String.class, pastaId),
        equalTo("Boil water"));

    NotebookGitBinding rebuiltBinding =
        notebookGitBindingRepository.findByNotebook_Id(notebookId).orElseThrow();
    assertThat(rebuiltBinding.getAcceptedGitObjectId(), not(equalTo(oldAcceptedGitObjectId)));
    assertThat(rebuiltBinding.getBundleBytes(), not(equalTo(oldBundleBytes)));
    List<PortableTreeEntry> treeEntries = readBundleTreeEntries(rebuiltBinding);
    assertThat(treeEntries.stream().map(PortableTreeEntry::path).toList(), hasSize(3));
    assertThat(
        treeEntries.stream().anyMatch(e -> e.path().equals("_trash/Recipes/Pasta.md")),
        equalTo(true));
    assertThat(
        treeEntries.stream().anyMatch(e -> e.path().equals("Recipes/Salad.md")), equalTo(true));
    assertThat(treeEntries.stream().anyMatch(e -> e.path().equals("README.md")), equalTo(true));
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void restartThroughFlywayPreservesACompletedBaselineRebuildUnchanged() throws Exception {
    User owner = makeMe.aUser().please();
    ownerUserIdsToClean.add(owner.getId());
    Notebook notebook =
        makeMe.aNotebook().creatorAndOwner(owner).readmeContent("# Notebook readme").please();
    Folder recipes = makeMe.aFolder().notebook(notebook).name("Recipes").please();
    makeMe.aNote("Salad").folder(recipes).content("Toss leaves").please();
    Integer notebookId = notebook.getId();

    Instant initialCutover = Instant.parse("2026-09-04T10:15:30Z");
    runBackfill(initialCutover);

    runBaselineRebuildMigration();
    NotebookGitBinding firstRebuild =
        notebookGitBindingRepository.findByNotebook_Id(notebookId).orElseThrow();
    List<PortableTreeEntry> firstTreeEntries = readBundleTreeEntries(firstRebuild);

    // Flyway records V300000327 as applied and skips it on restart, so the completed replacement
    // is preserved exactly. Re-invoking the migration directly (which Flyway would not do) rebuilds
    // the same content tree from current DB state; the commit is content-deterministic, so the
    // replacement is unchanged across a re-run.
    runBaselineRebuildMigration();
    NotebookGitBinding secondRebuild =
        notebookGitBindingRepository.findByNotebook_Id(notebookId).orElseThrow();
    List<PortableTreeEntry> secondTreeEntries = readBundleTreeEntries(secondRebuild);

    assertThat(
        secondTreeEntries.stream().map(PortableTreeEntry::path).toList(),
        equalTo(firstTreeEntries.stream().map(PortableTreeEntry::path).toList()));
    assertThat(
        secondTreeEntries.stream().map(PortableTreeEntry::content).toList(),
        equalTo(firstTreeEntries.stream().map(PortableTreeEntry::content).toList()));
    assertThat(
        secondRebuild.getAcceptedGitObjectId(), equalTo(firstRebuild.getAcceptedGitObjectId()));
    assertThat(secondRebuild.getBundleBytes(), equalTo(firstRebuild.getBundleBytes()));
  }

  private void runTrashMigration() throws Exception {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      new V300000326__MigrateLegacyDeletedNotesToTrash().migrate(flywayContext(connection));
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private void runBaselineRebuildMigration() throws Exception {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      new V300000327__RebuildNotebookGitBaselines().migrate(flywayContext(connection));
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private void runBackfill(Instant cutoverTime) throws Exception {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      NotebookGitFleetCutoverBackfill.run(connection, cutoverTime);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private Context flywayContext(Connection connection) {
    return new Context() {
      @Override
      public Connection getConnection() {
        return connection;
      }

      @Override
      public Configuration getConfiguration() {
        return null;
      }
    };
  }

  private String folderPathFromRoot(Integer folderId) {
    if (folderId == null) return "";
    List<String> names = new ArrayList<>();
    Integer currentId = folderId;
    while (currentId != null) {
      Integer[] nextHolder = new Integer[1];
      String name =
          jdbcTemplate.queryForObject(
              "SELECT name, parent_folder_id FROM folder WHERE id = ?",
              (rs, ignored) -> {
                int parentFolderId = rs.getInt("parent_folder_id");
                nextHolder[0] = rs.wasNull() ? null : parentFolderId;
                return rs.getString("name");
              },
              currentId);
      names.add(name);
      currentId = nextHolder[0];
    }
    java.util.Collections.reverse(names);
    return String.join("/", names);
  }

  private List<PortableTreeEntry> readBundleTreeEntries(NotebookGitBinding binding)
      throws Exception {
    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId headObjectId = GitBundleTestReader.fetchHead(readBack, binding.getBundleBytes());
      assertThat(headObjectId.getName(), equalTo(binding.getAcceptedGitObjectId()));
      try (RevWalk revWalk = new RevWalk(readBack)) {
        RevCommit commit = revWalk.parseCommit(headObjectId);
        assertThat(commit.getParentCount(), equalTo(0));
        List<PortableTreeEntry> entries = new ArrayList<>();
        try (TreeWalk treeWalk = new TreeWalk(readBack)) {
          treeWalk.addTree(commit.getTree());
          treeWalk.setRecursive(true);
          while (treeWalk.next()) {
            ObjectId blobId = treeWalk.getObjectId(0);
            ObjectLoader loader = readBack.open(blobId);
            String content = new String(loader.getBytes(), StandardCharsets.UTF_8);
            entries.add(new PortableTreeEntry(treeWalk.getPathString(), content));
          }
        }
        return entries;
      }
    }
  }
}
