package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import java.sql.Timestamp;
import java.time.Instant;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;

class NotebookGitWebTrashLinkedReferrerControllerTest
    extends NotebookGitWebContentControllerTestBase {
  static final Instant TRASH_AT = Instant.parse("2026-09-08T10:00:00Z");
  static final String CELLS_BODY = "---\ntype: Note\n---\ncells body";
  static final String REFERRER_BODY =
      "---\ntype: Note\nrelated: \"[[Biology/Cells|shown]]\"\n---\nSee [[Biology/Cells|shown]] for "
          + "details.";
  static final String PROPERTY_REFERRER_BODY = "---\ntarget: \"[[Target]]\"\n---\nBody";

  @Test
  void leaveDeadLinksTrashRetainsAuthoredReferrerSpellingInAcceptedTree() throws Exception {
    ReferrerTrashFixture f = seedCellsWithLinkedReferrer();
    testabilitySettings.timeTravelTo(Timestamp.from(TRASH_AT));

    noteController.trashNote(f.cells(), leaveDeadLinks());

    try (InMemoryRepository repo = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId downloadedHead =
          GitBundleTestReader.fetchHead(
              repo,
              controller
                  .downloadNotebookGitBundle(
                      notebookRepository.findById(f.notebook().getId()).orElseThrow())
                  .getBody());
      assertThat(
          GitBundleTestReader.pathsIn(repo, downloadedHead), hasItem("_trash/Biology/Cells.md"));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(repo, downloadedHead, "Reading.md"),
          equalTo(REFERRER_BODY));
    }
  }

  @Test
  void removeFromPropertiesTrashMatchesAuthoredReferrerAndTrashedTargetInAcceptedTree()
      throws Exception {
    PropertyReferrerTrashFixture f = seedTargetWithPropertyReferrer();
    testabilitySettings.timeTravelTo(Timestamp.from(TRASH_AT));

    noteController.trashNote(f.target(), removeFromProperties());

    String authoredReferrer =
        inCommittedTransaction(
            transactionManager,
            () -> noteRepository.findById(f.referrer().getId()).orElseThrow().getContent());
    try (InMemoryRepository repo = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId downloadedHead =
          GitBundleTestReader.fetchHead(
              repo,
              controller
                  .downloadNotebookGitBundle(
                      notebookRepository.findById(f.notebook().getId()).orElseThrow())
                  .getBody());
      assertThat(GitBundleTestReader.pathsIn(repo, downloadedHead), hasItem("_trash/Target.md"));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(repo, downloadedHead, "Referrer.md"),
          equalTo(authoredReferrer));
    }
  }

  ReferrerTrashFixture seedCellsWithLinkedReferrer() throws UnexpectedNoAccessRightException {
    Notebook notebook = createGitBackedNotebook();
    Folder biology = makeMe.aFolder().notebook(notebook).name("Biology").please();
    Note cells = makeMe.aNote("Cells").folder(biology).content(CELLS_BODY).please();
    Note referrer = makeMe.aNote("Reading").notebook(notebook).please();
    authorReferencingContentCommitted(referrer, REFERRER_BODY);
    snapshotCurrentPortableTree(notebook);
    return new ReferrerTrashFixture(notebook, cells);
  }

  record ReferrerTrashFixture(Notebook notebook, Note cells) {}

  PropertyReferrerTrashFixture seedTargetWithPropertyReferrer()
      throws UnexpectedNoAccessRightException {
    Notebook notebook = createGitBackedNotebook();
    Note target = makeMe.aNote("Target").notebook(notebook).please();
    Note referrer = makeMe.aNote("Referrer").notebook(notebook).please();
    authorReferencingContentCommitted(referrer, PROPERTY_REFERRER_BODY);
    snapshotCurrentPortableTree(notebook);
    return new PropertyReferrerTrashFixture(notebook, target, referrer);
  }

  record PropertyReferrerTrashFixture(Notebook notebook, Note target, Note referrer) {}
}
