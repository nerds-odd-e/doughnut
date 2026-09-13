package com.odde.donut.services.notebookGit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import com.odde.donut.testability.GitBundleTestReader;
import com.odde.donut.testability.MakeMe;
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
 * Exercises {@link NoteLegacyTrashMigration#run} against a real connection with no ambient Spring
 * test transaction, because the migration commits independently. Mirrors {@link
 * NotebookGitBaselineRebuildTest}'s raw-connection pattern and cleans up its own committed rows
 * explicitly.
 */
@SpringBootTest
@ActiveProfiles("test")
class NoteLegacyTrashMigrationTest {

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
          "DELETE FROM authored_note_reference WHERE source_note_id IN "
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
  void migratesALegacyDeletedLearnedNoteIntoTrashRetainingIdentityContentAndTrackers()
      throws Exception {
    User owner = makeMe.aUser().please();
    ownerUserIdsToClean.add(owner.getId());
    Notebook notebook =
        makeMe.aNotebook().creatorAndOwner(owner).readmeContent("# Notebook readme").please();
    Folder recipes = makeMe.aFolder().notebook(notebook).name("Recipes").please();
    Folder italian = makeMe.aFolder().parentFolder(recipes).name("Italian").please();
    Folder pasta = makeMe.aFolder().parentFolder(italian).name("Pasta").please();
    Note deletedLearnedNote =
        makeMe.aNote("Carbonara").folder(pasta).content("Guanciale eggs").please();
    jdbcTemplate.update(
        "INSERT INTO memory_tracker (user_id, note_id, assimilated_at, next_recall_at)"
            + " VALUES (?, ?, ?, ?)",
        owner.getId(),
        deletedLearnedNote.getId(),
        Timestamp.from(Instant.parse("2026-07-01T00:00:00Z")),
        Timestamp.from(Instant.parse("2026-07-01T00:00:00Z")));
    Integer trackerId =
        jdbcTemplate.queryForObject(
            "SELECT id FROM memory_tracker WHERE note_id = ? AND user_id = ?",
            Integer.class,
            deletedLearnedNote.getId(),
            owner.getId());
    Note liveNote = makeMe.aNote("Salad").folder(recipes).content("Toss leaves").please();
    Integer deletedNoteId = deletedLearnedNote.getId();
    Integer liveNoteId = liveNote.getId();
    Integer notebookId = notebook.getId();

    jdbcTemplate.update(
        "UPDATE note SET deleted_at = ? WHERE id = ?",
        Timestamp.from(Instant.parse("2026-08-01T00:00:00Z")),
        deletedNoteId);

    Instant migrationTime = Instant.parse("2026-09-13T12:00:00Z");
    runMigration(migrationTime);

    assertThat(
        jdbcTemplate.queryForObject(
            "SELECT deleted_at FROM note WHERE id = ?", Timestamp.class, deletedNoteId),
        nullValue());
    assertThat(
        jdbcTemplate.queryForObject(
            "SELECT title FROM note WHERE id = ?", String.class, deletedNoteId),
        equalTo("Carbonara"));
    assertThat(
        jdbcTemplate.queryForObject(
            "SELECT content FROM note WHERE id = ?", String.class, deletedNoteId),
        equalTo("Guanciale eggs"));
    assertThat(
        jdbcTemplate.queryForObject(
            "SELECT notebook_id FROM note WHERE id = ?", Integer.class, deletedNoteId),
        equalTo(notebookId));

    Integer migratedFolderId =
        jdbcTemplate.queryForObject(
            "SELECT folder_id FROM note WHERE id = ?", Integer.class, deletedNoteId);
    assertThat(migratedFolderId, notNullValue());
    assertThat(folderPathFromRoot(migratedFolderId), equalTo("_trash/Recipes/Italian/Pasta"));
    assertThat(isFolderTrashed(migratedFolderId), equalTo(true));

    assertThat(
        jdbcTemplate.queryForObject(
            "SELECT id FROM memory_tracker WHERE id = ?", Integer.class, trackerId),
        equalTo(trackerId));
    assertThat(
        jdbcTemplate.queryForObject(
            "SELECT note_id FROM memory_tracker WHERE id = ?", Integer.class, trackerId),
        equalTo(deletedNoteId));

    assertThat(
        jdbcTemplate.queryForObject(
            "SELECT title FROM note WHERE id = ?", String.class, liveNoteId),
        equalTo("Salad"));
    assertThat(
        jdbcTemplate.queryForObject(
            "SELECT deleted_at FROM note WHERE id = ?", Timestamp.class, liveNoteId),
        nullValue());
    assertThat(
        jdbcTemplate.queryForObject(
            "SELECT folder_id FROM note WHERE id = ?", Integer.class, liveNoteId),
        equalTo(recipes.getId()));

    assertThat(
        jdbcTemplate.queryForObject(
            "SELECT deleted_at FROM note WHERE id = ?", Timestamp.class, deletedNoteId),
        nullValue());
    assertThat(
        isFolderTrashed(
            jdbcTemplate.queryForObject(
                "SELECT folder_id FROM note WHERE id = ?", Integer.class, deletedNoteId)),
        equalTo(true));

    assertThat(
        jdbcTemplate.queryForObject(
            "SELECT removed_from_tracking FROM memory_tracker WHERE id = ?",
            Boolean.class,
            trackerId),
        equalTo(false));

    Instant backfillTime = Instant.parse("2026-09-04T10:15:30Z");
    runBackfill(backfillTime);
    Instant rebuildTime = Instant.parse("2026-09-13T12:30:00Z");
    runRebuild(notebookId, rebuildTime);

    NotebookGitBinding binding =
        notebookGitBindingRepository.findByNotebook_Id(notebookId).orElseThrow();
    List<PortableTreeEntry> treeEntries = readBundleTreeEntries(binding);
    assertThat(treeEntries.stream().map(PortableTreeEntry::path).toList(), hasSize(3));
    assertThat(
        treeEntries.stream()
            .anyMatch(e -> e.path().equals("_trash/Recipes/Italian/Pasta/Carbonara.md")),
        equalTo(true));
    assertThat(
        treeEntries.stream().anyMatch(e -> e.path().equals("Recipes/Salad.md")), equalTo(true));
    assertThat(treeEntries.stream().anyMatch(e -> e.path().equals("README.md")), equalTo(true));
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void migratesNotesInDeletedNotebooksWithoutClearingNotebookDeletion() throws Exception {
    User owner = makeMe.aUser().please();
    ownerUserIdsToClean.add(owner.getId());
    Notebook deletedNotebook =
        makeMe.aNotebook().creatorAndOwner(owner).readmeContent("# Deleted notebook").please();
    Folder topic = makeMe.aFolder().notebook(deletedNotebook).name("Topic").please();
    Note deletedNote = makeMe.aNote("Old").folder(topic).content("Legacy").please();
    Integer deletedNoteId = deletedNote.getId();
    Integer deletedNotebookId = deletedNotebook.getId();

    jdbcTemplate.update(
        "UPDATE notebook SET deleted_at = ? WHERE id = ?",
        Timestamp.from(Instant.parse("2026-07-01T00:00:00Z")),
        deletedNotebookId);
    jdbcTemplate.update(
        "UPDATE note SET deleted_at = ? WHERE id = ?",
        Timestamp.from(Instant.parse("2026-08-01T00:00:00Z")),
        deletedNoteId);

    Instant migrationTime = Instant.parse("2026-09-13T12:00:00Z");
    runMigration(migrationTime);

    assertThat(
        jdbcTemplate.queryForObject(
            "SELECT deleted_at FROM note WHERE id = ?", Timestamp.class, deletedNoteId),
        nullValue());
    Integer migratedFolderId =
        jdbcTemplate.queryForObject(
            "SELECT folder_id FROM note WHERE id = ?", Integer.class, deletedNoteId);
    assertThat(folderPathFromRoot(migratedFolderId), equalTo("_trash/Topic"));
    assertThat(
        jdbcTemplate.queryForObject(
            "SELECT deleted_at FROM notebook WHERE id = ?", Timestamp.class, deletedNotebookId),
        notNullValue());
    assertThat(
        notebookGitBindingRepository.findByNotebook_Id(deletedNotebookId).isPresent(),
        equalTo(false));
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void suffixedTitleWhenALiveTrashedNoteAlreadyHoldsTheSameTitleInTrashDestination()
      throws Exception {
    User owner = makeMe.aUser().please();
    ownerUserIdsToClean.add(owner.getId());
    Notebook notebook =
        makeMe.aNotebook().creatorAndOwner(owner).readmeContent("# Notebook readme").please();
    Folder recipes = makeMe.aFolder().notebook(notebook).name("Recipes").please();
    Note alreadyTrashed = makeMe.aNote("Pasta").folder(recipes).content("First").please();
    Folder trashRoot = makeMe.aFolder().notebook(notebook).name("_trash").please();
    Folder trashRecipes = makeMe.aFolder().parentFolder(trashRoot).name("Recipes").please();
    jdbcTemplate.update(
        "UPDATE note SET folder_id = ? WHERE id = ?", trashRecipes.getId(), alreadyTrashed.getId());
    Note legacyDeleted = makeMe.aNote("Pasta").folder(recipes).content("Second").please();
    Integer alreadyTrashedId = alreadyTrashed.getId();
    Integer legacyDeletedId = legacyDeleted.getId();

    jdbcTemplate.update(
        "UPDATE note SET deleted_at = ? WHERE id = ?",
        Timestamp.from(Instant.parse("2026-08-01T00:00:00Z")),
        legacyDeletedId);

    Instant migrationTime = Instant.parse("2026-09-13T12:00:00Z");
    runMigration(migrationTime);

    assertThat(
        jdbcTemplate.queryForObject(
            "SELECT title FROM note WHERE id = ?", String.class, alreadyTrashedId),
        equalTo("Pasta"));
    assertThat(
        jdbcTemplate.queryForObject(
            "SELECT title FROM note WHERE id = ?", String.class, legacyDeletedId),
        equalTo("Pasta (2)"));
    assertThat(
        folderPathFromRoot(
            jdbcTemplate.queryForObject(
                "SELECT folder_id FROM note WHERE id = ?", Integer.class, legacyDeletedId)),
        equalTo("_trash/Recipes"));
    assertThat(
        jdbcTemplate.queryForObject(
            "SELECT deleted_at FROM note WHERE id = ?", Timestamp.class, legacyDeletedId),
        nullValue());
  }

  private void runMigration(Instant migrationTime) throws Exception {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      NoteLegacyTrashMigration.run(connection, migrationTime);
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

  private void runRebuild(int notebookId, Instant rebuildTime) throws Exception {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      NotebookGitBaselineRebuild.rebuildNotebook(connection, notebookId, rebuildTime);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
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

  private Boolean isFolderTrashed(Integer folderId) {
    return jdbcTemplate.queryForObject(
        "SELECT id IN (SELECT id FROM trashed_folder) FROM folder WHERE id = ?",
        Boolean.class,
        folderId);
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
