package com.odde.donut.services.notebookGit;

import static com.odde.donut.services.notebookGit.NotebookGitRebuildTestSupport.assertBundleTreeEqualsCurrentContent;
import static com.odde.donut.services.notebookGit.NotebookGitRebuildTestSupport.deleteOwnedNotebookGraph;
import static com.odde.donut.services.notebookGit.NotebookGitRebuildTestSupport.runBackfill;
import static com.odde.donut.services.notebookGit.NotebookGitRebuildTestSupport.runRebuild;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.testability.GitBundleTestReader;
import com.odde.donut.testability.MakeMe;
import java.sql.SQLException;
import java.sql.Timestamp;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exercises {@link NotebookGitBaselineRebuild#rebuildNotebook} against a real connection with no
 * ambient Spring test transaction, because the rebuild commits its replacement binding
 * independently. Mirrors {@link NotebookGitFleetCutoverBackfillTest}'s raw-connection pattern and
 * cleans up its own committed rows explicitly.
 */
@SpringBootTest
@ActiveProfiles("test")
class NotebookGitBaselineRebuildTest {

  @Autowired MakeMe makeMe;
  @Autowired DataSource dataSource;
  @Autowired JdbcTemplate jdbcTemplate;
  @Autowired NotebookGitBindingRepository notebookGitBindingRepository;

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
  void replacesAnInconsistentBindingWithAFreshSingleRootSnapshotOfCurrentContent()
      throws Exception {
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
    Note pasta = makeMe.aNote("Pasta").folder(recipes).content("Boil water").please();
    Note salad = makeMe.aNote("Salad").folder(recipes).content("Toss leaves").please();

    // Establish an existing (initially consistent) binding via the fleet backfill.
    Instant initialCutover = Instant.parse("2026-09-04T10:15:30Z");
    runBackfill(dataSource, initialCutover);
    NotebookGitBinding initialBinding =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
    Integer bindingId = initialBinding.getId();
    String oldAcceptedGitObjectId = initialBinding.getAcceptedGitObjectId();
    byte[] oldBundleBytes = initialBinding.getBundleBytes();
    Timestamp oldCreatedAt = initialBinding.getCreatedAt();

    // Make the binding inconsistent: add a new live note and move Pasta into location-based trash
    // (a _trash folder subtree) so current DB content no longer matches the bound tree.
    makeMe.aNote("Soup").folder(recipes).content("Heat broth").please();
    Folder trashRoot = makeMe.aFolder().notebook(notebook).name("_trash").please();
    Folder trashRecipes = makeMe.aFolder().parentFolder(trashRoot).name("Recipes").please();
    jdbcTemplate.update(
        "UPDATE note SET folder_id = ? WHERE id = ?", trashRecipes.getId(), pasta.getId());

    Integer pastaId = pasta.getId();
    Integer saladId = salad.getId();
    Integer recipesFolderId = recipes.getId();
    Integer trashFolderId = trashRoot.getId();
    Integer trashRecipesFolderId = trashRecipes.getId();

    // Rebuild the baseline via the raw-JDBC operation.
    Instant rebuildTime = Instant.parse("2026-09-13T12:00:00Z");
    runRebuild(dataSource, notebook.getId(), rebuildTime);

    NotebookGitBinding rebuiltBinding =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();

    // Retained identity/ownership: same binding row id, same created_at, same notebook owner.
    assertThat(rebuiltBinding.getId(), equalTo(bindingId));
    assertThat(rebuiltBinding.getCreatedAt(), equalTo(oldCreatedAt));
    assertThat(
        jdbcTemplate.queryForObject(
            "SELECT creator_id FROM notebook WHERE id = ?", Integer.class, notebook.getId()),
        equalTo(owner.getId()));

    // Replaced accepted head and bundle; old head is gone.
    assertThat(rebuiltBinding.getAcceptedGitObjectId(), notNullValue());
    assertThat(rebuiltBinding.getAcceptedGitObjectId(), not(equalTo(oldAcceptedGitObjectId)));
    assertThat(rebuiltBinding.getBundleBytes(), not(equalTo(oldBundleBytes)));
    assertThat(rebuiltBinding.getUpdatedAt(), equalTo(Timestamp.from(rebuildTime)));

    // The new bundle has exactly one parentless root commit.
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

    // The new Portable tree equals the current DB content (readme, folders, live notes incl.
    // the trash-located Pasta note).
    assertBundleTreeEqualsCurrentContent(
        rebuiltBinding, notebook.getId(), dataSource, jdbcTemplate);

    // Retained database identities/data unchanged.
    assertThat(
        jdbcTemplate.queryForObject("SELECT title FROM note WHERE id = ?", String.class, pastaId),
        equalTo("Pasta"));
    assertThat(
        jdbcTemplate.queryForObject("SELECT content FROM note WHERE id = ?", String.class, pastaId),
        equalTo("Boil water"));
    assertThat(
        jdbcTemplate.queryForObject("SELECT content FROM note WHERE id = ?", String.class, saladId),
        equalTo("Toss leaves"));
    assertThat(
        jdbcTemplate.queryForObject(
            "SELECT id FROM folder WHERE id = ?", Integer.class, recipesFolderId),
        equalTo(recipesFolderId));
    assertThat(
        jdbcTemplate.queryForObject(
            "SELECT id FROM folder WHERE id = ?", Integer.class, trashFolderId),
        equalTo(trashFolderId));
    assertThat(
        jdbcTemplate.queryForObject(
            "SELECT id FROM folder WHERE id = ?", Integer.class, trashRecipesFolderId),
        equalTo(trashRecipesFolderId));
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void aMiddleRunFailureLeavesEachPersistedBindingConsistentAndRetryFinishesTheReset()
      throws Exception {
    User owner = makeMe.aUser().please();
    ownerUserIdsToClean.add(owner.getId());
    Notebook notebookA =
        makeMe.aNotebook().creatorAndOwner(owner).readmeContent("# A readme").please();
    Folder folderA = makeMe.aFolder().notebook(notebookA).name("Recipes").please();
    makeMe.aNote("Pasta").folder(folderA).content("Boil water").please();
    Notebook notebookB =
        makeMe.aNotebook().creatorAndOwner(owner).readmeContent("# B readme").please();
    makeMe.aNote("Salad").content("Toss leaves").please();
    Notebook notebookC =
        makeMe.aNotebook().creatorAndOwner(owner).readmeContent("# C readme").please();
    makeMe.aNote("Soup").content("Heat broth").please();

    Instant initialCutover = Instant.parse("2026-09-04T10:15:30Z");
    runBackfill(dataSource, initialCutover);

    NotebookGitBinding bindingA =
        notebookGitBindingRepository.findByNotebook_Id(notebookA.getId()).orElseThrow();
    NotebookGitBinding bindingB =
        notebookGitBindingRepository.findByNotebook_Id(notebookB.getId()).orElseThrow();
    String oldHeadA = bindingA.getAcceptedGitObjectId();
    byte[] oldBundleA = bindingA.getBundleBytes();
    Timestamp oldUpdatedAtA = bindingA.getUpdatedAt();
    String oldHeadB = bindingB.getAcceptedGitObjectId();
    byte[] oldBundleB = bindingB.getBundleBytes();
    Timestamp oldUpdatedAtB = bindingB.getUpdatedAt();

    makeMe.aNote("Bread").folder(folderA).content("Knead dough").please();
    makeMe.aNote("Dressing").content("Whisk oil").please();

    jdbcTemplate.update(
        "DELETE FROM notebook_git_binding WHERE notebook_id = ?", notebookC.getId());

    Instant failedRebuildTime = Instant.parse("2026-09-13T12:00:00Z");
    SQLException failure =
        assertThrows(
            SQLException.class, () -> runRebuild(dataSource, notebookC.getId(), failedRebuildTime));
    assertThat(
        failure.getMessage(),
        containsString("No notebook_git_binding row found for notebook_id=" + notebookC.getId()));

    NotebookGitBinding bindingAAfterFailure =
        notebookGitBindingRepository.findByNotebook_Id(notebookA.getId()).orElseThrow();
    NotebookGitBinding bindingBAfterFailure =
        notebookGitBindingRepository.findByNotebook_Id(notebookB.getId()).orElseThrow();
    assertThat(bindingAAfterFailure.getAcceptedGitObjectId(), equalTo(oldHeadA));
    assertThat(bindingAAfterFailure.getBundleBytes(), equalTo(oldBundleA));
    assertThat(bindingAAfterFailure.getUpdatedAt(), equalTo(oldUpdatedAtA));
    assertThat(bindingBAfterFailure.getAcceptedGitObjectId(), equalTo(oldHeadB));
    assertThat(bindingBAfterFailure.getBundleBytes(), equalTo(oldBundleB));
    assertThat(bindingBAfterFailure.getUpdatedAt(), equalTo(oldUpdatedAtB));
    assertThat(
        notebookGitBindingRepository.findByNotebook_Id(notebookC.getId()).isPresent(),
        equalTo(false));

    Instant retryTime = Instant.parse("2026-09-13T12:30:00Z");
    runRebuild(dataSource, notebookA.getId(), retryTime);
    runRebuild(dataSource, notebookB.getId(), retryTime);

    NotebookGitBinding rebuiltA =
        notebookGitBindingRepository.findByNotebook_Id(notebookA.getId()).orElseThrow();
    NotebookGitBinding rebuiltB =
        notebookGitBindingRepository.findByNotebook_Id(notebookB.getId()).orElseThrow();
    assertThat(rebuiltA.getAcceptedGitObjectId(), not(equalTo(oldHeadA)));
    assertThat(rebuiltA.getBundleBytes(), not(equalTo(oldBundleA)));
    assertThat(rebuiltA.getUpdatedAt(), equalTo(Timestamp.from(retryTime)));
    assertThat(rebuiltB.getAcceptedGitObjectId(), not(equalTo(oldHeadB)));
    assertThat(rebuiltB.getBundleBytes(), not(equalTo(oldBundleB)));
    assertThat(rebuiltB.getUpdatedAt(), equalTo(Timestamp.from(retryTime)));

    assertBundleTreeEqualsCurrentContent(rebuiltA, notebookA.getId(), dataSource, jdbcTemplate);
    assertBundleTreeEqualsCurrentContent(rebuiltB, notebookB.getId(), dataSource, jdbcTemplate);
  }
}
