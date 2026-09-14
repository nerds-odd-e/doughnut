package com.odde.donut.services.notebookGit;

import static com.odde.donut.services.notebookGit.NotebookGitRebuildTestSupport.assertBundleTreeEqualsCurrentContent;
import static com.odde.donut.services.notebookGit.NotebookGitRebuildTestSupport.deleteOwnedNotebookGraph;
import static com.odde.donut.services.notebookGit.NotebookGitRebuildTestSupport.runBackfill;
import static com.odde.donut.services.notebookGit.NotebookGitRebuildTestSupport.runRebuild;
import static com.odde.donut.services.notebookGit.NotebookUpgradeDataManifest.capture;
import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.testability.GitBundleTestReader;
import com.odde.donut.testability.MakeMe;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rehearsal of the actual isolated upgrade (registered Flyway V300000326–V300000328) against a
 * representative notebook, proving retained notebook data survives the baseline rebuild while the
 * Git base is replaced with a fresh single-root snapshot. Mirrors {@link
 * NotebookGitBaselineRebuildTest}'s raw-connection pattern and cleans up its own committed rows
 * explicitly.
 *
 * <p>Recorded findings:
 *
 * <ul>
 *   <li>Schema versions exercised: V300000326 (trash migration), V300000327 (baseline rebuild),
 *       V300000328 (drop deleted_at). The trash migration and column drop are already registered
 *       and applied at startup; this test creates data after startup and so manually re-runs the
 *       baseline rebuild (V300000327's operation) on the freshly created notebook.
 *   <li>Fixture coverage: live notes in nested folders, trashed notes under a nested _trash
 *       subtree, memory trackers (spelling), authored wiki-link references, notebook readme, and
 *       folder readmes at multiple depths.
 *   <li>Observed outcome: every retained row (notes, memory trackers, folders, authored references)
 *       is byte-for-byte unchanged across the rebuild; the new bundle has exactly one parentless
 *       root commit with no old history; its complete Portable tree equals the current database
 *       content (live + trashed notes, folders, readmes).
 *   <li>Limitation: the later release that applies this upgrade to production must exclude
 *       concurrent old-app writes and provide owners with a fresh checkout. This test does not
 *       implement a maintenance-mode product feature or perform a release.
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
class NotebookUpgradeDataPreservationTest {

  @Autowired MakeMe makeMe;
  @Autowired DataSource dataSource;
  @Autowired JdbcTemplate jdbcTemplate;
  @Autowired NotebookGitBindingRepository notebookGitBindingRepository;
  @Autowired NoteRepository noteRepository;
  @Autowired PlatformTransactionManager transactionManager;

  private final List<Integer> ownerUserIdsToClean = new ArrayList<>();

  @AfterEach
  void cleanUp() {
    for (Integer ownerUserId : ownerUserIdsToClean) {
      deleteOwnedNotebookGraph(jdbcTemplate, ownerUserId);
    }
    ownerUserIdsToClean.clear();
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void theRehearsedUpgradePreservesAllRetainedNotebookDataWhileReplacingTheGitBase()
      throws Exception {
    User owner = makeMe.aUser().please();
    ownerUserIdsToClean.add(owner.getId());
    Notebook notebook =
        makeMe.aNotebook().creatorAndOwner(owner).readmeContent("# Notebook readme").please();
    int notebookId = notebook.getId();

    Folder recipes =
        makeMe
            .aFolder()
            .notebook(notebook)
            .name("Recipes")
            .readmeContent("# Recipes readme")
            .please();
    Folder italian =
        makeMe
            .aFolder()
            .parentFolder(recipes)
            .name("Italian")
            .readmeContent("# Italian readme")
            .please();
    Note pasta = makeMe.aNote("Pasta").folder(italian).content("Boil water and salt").please();
    Note salad = makeMe.aNote("Salad").folder(recipes).content("Toss leaves").please();

    // Trashed note under a nested _trash/Recipes/Italian subtree (mirrors the V300000326 migration
    // destination for a legacy deleted note originally in Recipes/Italian).
    Folder trashRoot = makeMe.aFolder().notebook(notebook).name("_trash").please();
    Folder trashRecipes = makeMe.aFolder().parentFolder(trashRoot).name("Recipes").please();
    Folder trashItalian = makeMe.aFolder().parentFolder(trashRecipes).name("Italian").please();
    Note trashedPasta =
        makeMe.aNote("Old Pasta").folder(trashItalian).content("Legacy boiled pasta").please();

    // Memory trackers on a live note and a trashed note.
    inCommittedTransaction(
        transactionManager,
        () ->
            makeMe
                .aMemoryTrackerFor(noteRepository.findById(pasta.getId()).orElseThrow())
                .spelling()
                .please());
    inCommittedTransaction(
        transactionManager,
        () ->
            makeMe
                .aMemoryTrackerFor(noteRepository.findById(trashedPasta.getId()).orElseThrow())
                .spelling()
                .please());

    // Authored wiki-link reference from Salad to Pasta.
    inCommittedTransaction(
        transactionManager,
        () ->
            makeMe.authorReferencingContent(
                noteRepository.findById(salad.getId()).orElseThrow(), "[[Pasta]]"));

    // Establish the notebook's Git binding via the fleet backfill (runs at startup in production;
    // here the notebook is created after startup, so the backfill is re-run manually).
    Instant initialCutover = Instant.parse("2026-09-04T10:15:30Z");
    runBackfill(dataSource, initialCutover);

    // Capture the retained-data manifest BEFORE the baseline rebuild.
    NotebookUpgradeDataManifest dataBefore = capture(jdbcTemplate, notebookId);

    // Run the baseline rebuild (V300000327's operation) via the raw-JDBC entry point.
    Instant rebuildTime = Instant.parse("2026-09-13T12:00:00Z");
    runRebuild(dataSource, notebookId, rebuildTime);

    // Capture the retained-data manifest AFTER the rebuild and assert it matches exactly.
    NotebookUpgradeDataManifest dataAfter = capture(jdbcTemplate, notebookId);
    assertThat(dataAfter, equalTo(dataBefore));

    // The new bundle has exactly one reachable root commit (no old history).
    NotebookGitBinding rebuiltBinding =
        notebookGitBindingRepository.findByNotebook_Id(notebookId).orElseThrow();
    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId headObjectId =
          GitBundleTestReader.fetchHead(readBack, rebuiltBinding.getBundleBytes());
      try (RevWalk revWalk = new RevWalk(readBack)) {
        RevCommit commit = revWalk.parseCommit(headObjectId);
        revWalk.reset();
        revWalk.markStart(commit);
        int commitCount = 0;
        for (RevCommit ignored : revWalk) {
          commitCount++;
        }
        assertThat(commitCount, equalTo(1));
      }
    }

    // The new bundle's head is parentless and its complete Portable tree equals the current DB
    // content (live + trashed notes, folders, readmes).
    assertBundleTreeEqualsCurrentContent(rebuiltBinding, notebookId, dataSource, jdbcTemplate);
  }
}
