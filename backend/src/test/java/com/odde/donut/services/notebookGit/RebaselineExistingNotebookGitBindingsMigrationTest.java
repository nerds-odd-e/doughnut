package com.odde.donut.services.notebookGit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.RecallLog;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.services.notebookExport.ExportFolderRow;
import com.odde.donut.services.notebookExport.ExportNoteRow;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import com.odde.donut.services.notebookExport.PortableTreeSnapshot;
import com.odde.donut.testability.CommittedTransactionTestSupport;
import com.odde.donut.testability.GitBundleTestReader;
import com.odde.donut.testability.MakeMe;
import db.migration.V300000330__RebaselineExistingNotebookGitBindings;
import java.sql.Connection;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exercises the real, registered {@link V300000330__RebaselineExistingNotebookGitBindings} Flyway
 * migration directly against a real connection with no ambient Spring test transaction, because the
 * migration commits each notebook's replacement binding independently via {@link
 * NotebookGitBaselineRebuild#rebuildNotebook}. Fixture setup runs in its own committed transaction
 * (so JPA entities stay attached to one session while being built) via {@link
 * CommittedTransactionTestSupport}, and the test cleans up its own committed rows explicitly.
 */
@SpringBootTest
@ActiveProfiles("test")
class RebaselineExistingNotebookGitBindingsMigrationTest {

  @Autowired MakeMe makeMe;
  @Autowired DataSource dataSource;
  @Autowired JdbcTemplate jdbcTemplate;
  @Autowired NotebookGitBindingRepository notebookGitBindingRepository;
  @Autowired NotebookGitCutoverService notebookGitCutoverService;
  @Autowired PlatformTransactionManager transactionManager;

  private final List<Integer> ownerUserIdsToClean = new ArrayList<>();

  private record CanonicalFixture(
      int notebookId,
      int bindingId,
      Timestamp createdAt,
      String oldRootObjectId,
      byte[] initialBundleBytes,
      int pastaId,
      int draftId,
      int recipesFolderId,
      int techniquesFolderId,
      int trashRootFolderId,
      int trashRecipesFolderId,
      int memoryTrackerId,
      int recallLogId) {}

  @AfterEach
  void cleanUp() {
    for (Integer ownerUserId : ownerUserIdsToClean) {
      jdbcTemplate.update(
          "DELETE FROM notebook_git_binding WHERE notebook_id IN "
              + "(SELECT id FROM notebook WHERE creator_id = ?)",
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
  void replacesEveryLiveBoundNotebooksCompleteOldHistoryWithACurrentSingleRootSnapshot()
      throws Exception {
    // Canonical fixture: readme, nested folder with its own readme, an ordinary note, a note
    // relocated into a _trash folder subtree, and retained learning data. Built inside one
    // committed transaction so the JPA entities stay attached to a single session throughout.
    CanonicalFixture fixture =
        CommittedTransactionTestSupport.inCommittedTransaction(
            transactionManager, this::buildCanonicalFixture);

    // A second, simpler live bound notebook to prove fleet iteration over more than one notebook.
    record SecondNotebook(int notebookId, String oldHead) {}
    SecondNotebook second =
        CommittedTransactionTestSupport.inCommittedTransaction(
            transactionManager,
            () -> {
              User owner = makeMe.aUser().please();
              ownerUserIdsToClean.add(owner.getId());
              Notebook secondNotebook =
                  makeMe
                      .aNotebook()
                      .creatorAndOwner(owner)
                      .readmeContent("# Second readme")
                      .please();
              makeMe.aNote("Only").notebook(secondNotebook).content("Only content").please();
              makeMe.entityPersister.flush();
              NotebookGitBinding secondInitialBinding =
                  notebookGitCutoverService.createBindingForNotebook(
                      secondNotebook, Instant.parse("2026-09-02T00:00:00Z"));
              return new SecondNotebook(
                  secondNotebook.getId(), secondInitialBinding.getAcceptedGitObjectId());
            });

    // Layer a later "accepted" commit on top of the original root, so the canonical fixture's
    // history is genuinely multi-commit before migration runs.
    List<PortableTreeEntry> laterEntries =
        PortableTreeSnapshot.build(
            "# Notebook readme",
            List.of(
                new ExportFolderRow(fixture.recipesFolderId(), null, "Recipes", "# Recipes readme"),
                new ExportFolderRow(
                    fixture.techniquesFolderId(),
                    fixture.recipesFolderId(),
                    "Techniques",
                    "# Techniques readme"),
                new ExportFolderRow(fixture.trashRootFolderId(), null, "_trash", null),
                new ExportFolderRow(
                    fixture.trashRecipesFolderId(), fixture.trashRootFolderId(), "Recipes", null)),
            List.of(
                new ExportNoteRow(fixture.recipesFolderId(), "Pasta", "Boil water"),
                new ExportNoteRow(fixture.trashRecipesFolderId(), "Draft", "Draft content")));
    Instant laterAcceptedTime = Instant.parse("2026-09-10T08:00:00Z");
    ObjectId oldRootObjectId = ObjectId.fromString(fixture.oldRootObjectId());
    ObjectId oldLaterAcceptedObjectId;
    byte[] multiCommitBundleBytes;
    try (InMemoryRepository appendRepository =
        new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.fetchHead(appendRepository, fixture.initialBundleBytes());
      oldLaterAcceptedObjectId =
          NotebookGitBundleBuilder.append(
              appendRepository,
              oldRootObjectId,
              laterEntries,
              NotebookGitCutoverService.SYSTEM_AUTHOR_NAME,
              NotebookGitCutoverService.SYSTEM_AUTHOR_EMAIL,
              "A later locally accepted change",
              laterAcceptedTime);
      NotebookGitBundleWriter.BundleWriteResult writtenLater =
          NotebookGitBundleWriter.write(appendRepository);
      multiCommitBundleBytes = writtenLater.bundleBytes();
    }
    jdbcTemplate.update(
        "UPDATE notebook_git_binding SET accepted_git_object_id = ?, bundle_bytes = ?,"
            + " updated_at = ? WHERE notebook_id = ?",
        oldLaterAcceptedObjectId.getName(),
        multiCommitBundleBytes,
        Timestamp.from(laterAcceptedTime),
        fixture.notebookId());

    // Run the real, registered migration directly against a real JDBC connection.
    runMigration(dataSource);

    // --- Canonical fixture assertions ---
    NotebookGitBinding rebuiltBinding =
        notebookGitBindingRepository.findByNotebook_Id(fixture.notebookId()).orElseThrow();

    // Retained binding identity.
    assertThat(rebuiltBinding.getId(), equalTo(fixture.bindingId()));
    assertThat(rebuiltBinding.getCreatedAt(), equalTo(fixture.createdAt()));

    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId headObjectId =
          GitBundleTestReader.fetchHead(readBack, rebuiltBinding.getBundleBytes());

      // Head and bundle remain one consistent replacement.
      assertThat(headObjectId.getName(), equalTo(rebuiltBinding.getAcceptedGitObjectId()));

      try (RevWalk revWalk = new RevWalk(readBack)) {
        RevCommit commit = revWalk.parseCommit(headObjectId);

        // Exactly one commit is reachable from the new head: a parentless root.
        assertThat(commit.getParentCount(), equalTo(0));
        revWalk.reset();
        revWalk.markStart(commit);
        int commitCount = 0;
        for (RevCommit ignored : revWalk) {
          commitCount++;
        }
        assertThat(commitCount, equalTo(1));

        // The captured pre-migration old root and old later accepted commit cannot be resolved
        // from the new bundle: complete history abandonment, not just a new head pointer.
        revWalk.reset();
        assertThrows(MissingObjectException.class, () -> revWalk.parseCommit(oldRootObjectId));
        revWalk.reset();
        assertThrows(
            MissingObjectException.class, () -> revWalk.parseCommit(oldLaterAcceptedObjectId));

        // The new tree matches the notebook's current Portable-tree content.
        List<PortableTreeEntry> foundEntries =
            GitBundleTestReader.readTreeEntries(readBack, commit);
        List<PortableTreeEntry> sortedExpected =
            laterEntries.stream().sorted((a, b) -> a.path().compareTo(b.path())).toList();
        assertThat(foundEntries, contains(sortedExpected.toArray(new PortableTreeEntry[0])));
      }
    }

    // Retained database identities/content and learning data survive unchanged.
    assertThat(
        jdbcTemplate.queryForObject(
            "SELECT title FROM note WHERE id = ?", String.class, fixture.pastaId()),
        equalTo("Pasta"));
    assertThat(
        jdbcTemplate.queryForObject(
            "SELECT content FROM note WHERE id = ?", String.class, fixture.pastaId()),
        equalTo("Boil water"));
    assertThat(
        jdbcTemplate.queryForObject(
            "SELECT content FROM note WHERE id = ?", String.class, fixture.draftId()),
        equalTo("Draft content"));
    assertThat(
        jdbcTemplate.queryForObject(
            "SELECT folder_id FROM note WHERE id = ?", Integer.class, fixture.draftId()),
        equalTo(fixture.trashRecipesFolderId()));
    assertThat(
        jdbcTemplate.queryForObject(
            "SELECT id FROM folder WHERE id = ?", Integer.class, fixture.recipesFolderId()),
        equalTo(fixture.recipesFolderId()));
    assertThat(
        jdbcTemplate.queryForObject(
            "SELECT id FROM folder WHERE id = ?", Integer.class, fixture.techniquesFolderId()),
        equalTo(fixture.techniquesFolderId()));

    assertThat(
        jdbcTemplate.queryForObject(
            "SELECT note_id FROM memory_tracker WHERE id = ?",
            Integer.class,
            fixture.memoryTrackerId()),
        equalTo(fixture.pastaId()));
    assertThat(
        jdbcTemplate.queryForObject(
            "SELECT memory_tracker_id FROM recall_log WHERE id = ?",
            Integer.class,
            fixture.recallLogId()),
        equalTo(fixture.memoryTrackerId()));

    // --- Second notebook: fleet iteration over more than one notebook ---
    NotebookGitBinding secondRebuiltBinding =
        notebookGitBindingRepository.findByNotebook_Id(second.notebookId()).orElseThrow();
    assertThat(secondRebuiltBinding.getAcceptedGitObjectId(), notNullValue());
    assertThat(secondRebuiltBinding.getAcceptedGitObjectId(), not(equalTo(second.oldHead())));
    try (InMemoryRepository readBackSecond =
        new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId secondHeadObjectId =
          GitBundleTestReader.fetchHead(readBackSecond, secondRebuiltBinding.getBundleBytes());
      assertThat(
          secondHeadObjectId.getName(), equalTo(secondRebuiltBinding.getAcceptedGitObjectId()));
      try (RevWalk revWalk = new RevWalk(readBackSecond)) {
        RevCommit commit = revWalk.parseCommit(secondHeadObjectId);
        assertThat(commit.getParentCount(), equalTo(0));
      }
    }
  }

  private CanonicalFixture buildCanonicalFixture() {
    User owner = makeMe.aUser().please();
    ownerUserIdsToClean.add(owner.getId());
    Notebook notebook =
        makeMe.aNotebook().creatorAndOwner(owner).readmeContent("# Notebook readme").please();
    Folder recipes =
        makeMe
            .aFolder()
            .notebook(notebook)
            .name("Recipes")
            .readmeContent("# Recipes readme")
            .please();
    Folder techniques =
        makeMe
            .aFolder()
            .parentFolder(recipes)
            .name("Techniques")
            .readmeContent("# Techniques readme")
            .please();
    Note pasta = makeMe.aNote("Pasta").folder(recipes).content("Boil water").please();
    MemoryTracker memoryTracker = makeMe.aMemoryTrackerFor(pasta).please();
    RecallLog recallLog = makeMe.aRecallLogFor(memoryTracker).please();
    makeMe.entityPersister.flush();

    // Establish the very first accepted root via the current creation-time path.
    NotebookGitBinding initialBinding =
        notebookGitCutoverService.createBindingForNotebook(
            notebook, Instant.parse("2026-09-01T00:00:00Z"));

    // Add a later note, then relocate it into location-based trash so the accepted history moves
    // past its original root before migration runs.
    Note draft = makeMe.aNote("Draft").folder(recipes).content("Draft content").please();
    Folder trashRoot = makeMe.aFolder().notebook(notebook).name("_trash").please();
    Folder trashRecipes = makeMe.aFolder().parentFolder(trashRoot).name("Recipes").please();
    jdbcTemplate.update(
        "UPDATE note SET folder_id = ? WHERE id = ?", trashRecipes.getId(), draft.getId());

    return new CanonicalFixture(
        notebook.getId(),
        initialBinding.getId(),
        initialBinding.getCreatedAt(),
        initialBinding.getAcceptedGitObjectId(),
        initialBinding.getBundleBytes(),
        pasta.getId(),
        draft.getId(),
        recipes.getId(),
        techniques.getId(),
        trashRoot.getId(),
        trashRecipes.getId(),
        memoryTracker.getId(),
        recallLog.getId());
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void leavesDeletedAndUnboundNotebooksOutsideTheMigratedFleet() throws Exception {
    record DeletedNotebookFixture(
        int notebookId, String oldHead, byte[] oldBundleBytes, Timestamp oldCreatedAt) {}

    DeletedNotebookFixture deletedFixture =
        CommittedTransactionTestSupport.inCommittedTransaction(
            transactionManager,
            () -> {
              User owner = makeMe.aUser().please();
              ownerUserIdsToClean.add(owner.getId());
              Notebook deletedNotebook =
                  makeMe
                      .aNotebook()
                      .creatorAndOwner(owner)
                      .readmeContent("# Deleted readme")
                      .please();
              makeMe
                  .aNote("Deleted note")
                  .notebook(deletedNotebook)
                  .content("Deleted content")
                  .please();
              makeMe.entityPersister.flush();
              NotebookGitBinding deletedBinding =
                  notebookGitCutoverService.createBindingForNotebook(
                      deletedNotebook, Instant.parse("2026-09-03T00:00:00Z"));
              return new DeletedNotebookFixture(
                  deletedNotebook.getId(),
                  deletedBinding.getAcceptedGitObjectId(),
                  deletedBinding.getBundleBytes(),
                  deletedBinding.getCreatedAt());
            });
    Timestamp oldUpdatedAt =
        jdbcTemplate.queryForObject(
            "SELECT updated_at FROM notebook_git_binding WHERE notebook_id = ?",
            Timestamp.class,
            deletedFixture.notebookId());
    jdbcTemplate.update(
        "UPDATE notebook SET deleted_at = ? WHERE id = ?",
        Timestamp.from(Instant.parse("2026-09-04T00:00:00Z")),
        deletedFixture.notebookId());

    // A live notebook with no binding at all.
    int unboundNotebookId =
        CommittedTransactionTestSupport.inCommittedTransaction(
            transactionManager,
            () -> {
              User owner = makeMe.aUser().please();
              ownerUserIdsToClean.add(owner.getId());
              Notebook unboundNotebook =
                  makeMe
                      .aNotebook()
                      .creatorAndOwner(owner)
                      .readmeContent("# Unbound readme")
                      .please();
              return unboundNotebook.getId();
            });

    runMigration(dataSource);

    NotebookGitBinding untouchedBinding =
        notebookGitBindingRepository.findByNotebook_Id(deletedFixture.notebookId()).orElseThrow();
    assertThat(untouchedBinding.getAcceptedGitObjectId(), equalTo(deletedFixture.oldHead()));
    assertThat(untouchedBinding.getBundleBytes(), equalTo(deletedFixture.oldBundleBytes()));
    assertThat(untouchedBinding.getCreatedAt(), equalTo(deletedFixture.oldCreatedAt()));
    assertThat(untouchedBinding.getUpdatedAt(), equalTo(oldUpdatedAt));

    assertTrue(notebookGitBindingRepository.findByNotebook_Id(unboundNotebookId).isEmpty());
  }

  private static void runMigration(DataSource dataSource) throws Exception {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      new V300000330__RebaselineExistingNotebookGitBindings()
          .migrate(
              new Context() {
                @Override
                public Configuration getConfiguration() {
                  throw new UnsupportedOperationException("not needed by this migration");
                }

                @Override
                public Connection getConnection() {
                  return connection;
                }
              });
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }
}
