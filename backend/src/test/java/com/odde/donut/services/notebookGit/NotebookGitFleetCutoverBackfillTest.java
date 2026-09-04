package com.odde.donut.services.notebookGit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.testability.MakeMe;
import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.FetchConnection;
import org.eclipse.jgit.transport.TransportBundleStream;
import org.eclipse.jgit.transport.URIish;
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
 * Exercises {@link NotebookGitFleetCutoverBackfill#run} against a real connection with no ambient
 * Spring test transaction, because the backfill commits each notebook's binding independently.
 * Every test cleans up its own committed rows explicitly.
 */
@SpringBootTest
@ActiveProfiles("test")
class NotebookGitFleetCutoverBackfillTest {

  @Autowired MakeMe makeMe;
  @Autowired DataSource dataSource;
  @Autowired JdbcTemplate jdbcTemplate;
  @Autowired NotebookGitBindingRepository notebookGitBindingRepository;

  private final List<Integer> ownerUserIdsToClean = new ArrayList<>();

  @AfterEach
  void cleanUp() {
    for (Integer ownerUserId : ownerUserIdsToClean) {
      jdbcTemplate.update(
          "DELETE FROM note WHERE notebook_id IN "
              + "(SELECT id FROM notebook WHERE creator_id = ?)",
          ownerUserId);
      jdbcTemplate.update("DELETE FROM user WHERE id = ?", ownerUserId);
    }
    ownerUserIdsToClean.clear();
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void bindsEveryLiveNotebookExactlyOnceAndRetryCreatesNoDuplicate() throws Exception {
    Notebook first = aNotebookWithContent("# First readme", "Recipes", "# Recipes readme");
    Notebook second = aNotebookWithContent("# Second readme", "Travel", "# Travel readme");
    Notebook deleted = aNotebookWithContent("# Deleted readme", "Old", "# Old readme");
    jdbcTemplate.update("UPDATE notebook SET deleted_at = NOW() WHERE id = ?", deleted.getId());

    Instant cutoverTime = Instant.parse("2026-09-04T10:15:30Z");
    runBackfill(cutoverTime);

    NotebookGitBinding firstBinding = assertSingleRootCommitBinding(first);
    NotebookGitBinding secondBinding = assertSingleRootCommitBinding(second);
    assertThat(
        notebookGitBindingRepository.findByNotebook_Id(deleted.getId()).isPresent(),
        equalTo(false));

    // Re-running is idempotent: no second binding, no changed accepted head.
    runBackfill(cutoverTime.plusSeconds(60));

    NotebookGitBinding firstAfterRetry =
        notebookGitBindingRepository.findByNotebook_Id(first.getId()).orElseThrow();
    NotebookGitBinding secondAfterRetry =
        notebookGitBindingRepository.findByNotebook_Id(second.getId()).orElseThrow();
    assertThat(firstAfterRetry.getId(), equalTo(firstBinding.getId()));
    assertThat(
        firstAfterRetry.getAcceptedGitObjectId(), equalTo(firstBinding.getAcceptedGitObjectId()));
    assertThat(secondAfterRetry.getId(), equalTo(secondBinding.getId()));
    assertThat(
        secondAfterRetry.getAcceptedGitObjectId(), equalTo(secondBinding.getAcceptedGitObjectId()));
    assertThat(
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM notebook_git_binding WHERE notebook_id IN (?, ?)",
            Integer.class,
            first.getId(),
            second.getId()),
        equalTo(2));
  }

  private Notebook aNotebookWithContent(
      String notebookReadme, String folderName, String folderReadme) {
    User owner = makeMe.aUser().please();
    ownerUserIdsToClean.add(owner.getId());
    Notebook notebook =
        makeMe.aNotebook().creatorAndOwner(owner).readmeContent(notebookReadme).please();
    Folder folder =
        makeMe.aFolder().notebook(notebook).name(folderName).readmeContent(folderReadme).please();
    makeMe.aNote("Pasta").folder(folder).content("Boil water").please();
    return notebook;
  }

  private void runBackfill(Instant cutoverTime) throws Exception {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      NotebookGitFleetCutoverBackfill.run(connection, cutoverTime);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private NotebookGitBinding assertSingleRootCommitBinding(Notebook notebook) throws Exception {
    NotebookGitBinding binding =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
    assertThat(binding, notNullValue());

    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId headObjectId = fetchBundleInto(readBack, binding.getBundleBytes());
      assertThat(headObjectId.getName(), equalTo(binding.getAcceptedGitObjectId()));

      try (RevWalk revWalk = new RevWalk(readBack)) {
        RevCommit commit = revWalk.parseCommit(headObjectId);
        assertThat(commit.getParentCount(), equalTo(0));
        assertThat(commit.getAuthorIdent().getName(), equalTo("Donut System"));
        assertThat(commit.getAuthorIdent().getEmailAddress(), equalTo("system@donut.local"));

        revWalk.reset();
        revWalk.markStart(commit);
        int commitCount = 0;
        for (RevCommit ignored : revWalk) {
          commitCount++;
        }
        assertThat(commitCount, equalTo(1));
      }
    }
    return binding;
  }

  private static ObjectId fetchBundleInto(InMemoryRepository target, byte[] bundleBytes)
      throws Exception {
    try (TransportBundleStream transport =
            new TransportBundleStream(
                target, new URIish("in-memory:bundle"), new ByteArrayInputStream(bundleBytes));
        FetchConnection fetchConnection = transport.openFetch()) {
      Ref mainRef = fetchConnection.getRef("refs/heads/main");
      fetchConnection.fetch(NullProgressMonitor.INSTANCE, List.of(mainRef), java.util.Set.of());
      return mainRef.getObjectId();
    }
  }
}
