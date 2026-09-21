package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.testability.GitBundleTestReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** Migration-only: remove once production has applied the containment repair. */
class NotebookFollowsFolderContainmentMigrationTest extends NotebookGitControllerTestBase {

  private static final String MIGRATION =
      "db/migration/V300000333__notebook_follows_folder_containment.sql";
  private static final String ORIGINAL = "---\ntype: Note\n---\nOriginal.\n";
  private static final String EDITED = "---\ntype: Note\n---\nEdited.\n";

  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void strayRowsJoinTheirContainingNotebookAndTheirFormerNotebookPublishesAgain() throws Exception {
    Notebook notebook = createGitBackedNotebook("Owns Stray Rows");
    Notebook container = createGitBackedNotebook("Contains Stray Rows");
    Note root = makeMe.aNote().notebook(notebook).title("Root").content(ORIGINAL).please();
    Folder foreign = makeMe.aFolder().notebook(container).name("Foreign").please();
    Folder stray = makeMe.aFolder().notebook(notebook).name("Stray").please();
    Note inStray = makeMe.aNote().folder(stray).title("In Stray").content(ORIGINAL).please();
    Note inForeign =
        makeMe.aNote().notebook(notebook).title("In Foreign").content(ORIGINAL).please();
    inCommittedTransaction(
        transactionManager,
        () -> {
          entityManager
              .createNativeQuery("UPDATE folder SET parent_folder_id = :parent WHERE id = :id")
              .setParameter("parent", foreign.getId())
              .setParameter("id", stray.getId())
              .executeUpdate();
          entityManager
              .createNativeQuery("UPDATE note SET folder_id = :folder WHERE id = :id")
              .setParameter("folder", foreign.getId())
              .setParameter("id", inForeign.getId())
              .executeUpdate();
        });
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    String acceptedHead = binding.getAcceptedGitObjectId();
    byte[] rootEdit =
        proposalBundleBytes(binding, List.of(new NotebookGitProposalFile("Root.md", EDITED)));

    assertProposalRejectedWithoutMutatingBinding(
        notebook, acceptedHead, rootEdit, NullPointerException.class);

    inCommittedTransaction(
        transactionManager, () -> migrationStatements().forEach(jdbcTemplate::update));

    assertThat(notebookIdOf("folder", stray.getId()), equalTo(container.getId()));
    assertThat(notebookIdOf("note", inStray.getId()), equalTo(container.getId()));
    assertThat(notebookIdOf("note", inForeign.getId()), equalTo(container.getId()));
    assertThat(notebookIdOf("note", root.getId()), equalTo(notebook.getId()));
    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription())) {
      String proposedHead =
          GitBundleTestReader.fetchSingleParentCommit(proposal, rootEdit).head().getName();
      assertThat(
          controller.publishNotebookGitProposal(notebook.getId(), acceptedHead, rootEdit),
          equalTo(proposedHead));
    }
  }

  private Integer notebookIdOf(String table, Integer id) {
    return jdbcTemplate.queryForObject(
        "SELECT notebook_id FROM " + table + " WHERE id = ?", Integer.class, id);
  }

  private static List<String> migrationStatements() {
    try (InputStream in =
        NotebookFollowsFolderContainmentMigrationTest.class
            .getClassLoader()
            .getResourceAsStream(MIGRATION)) {
      return List.of(new String(in.readAllBytes(), StandardCharsets.UTF_8).split(";\\s*"));
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read " + MIGRATION, e);
    }
  }
}
