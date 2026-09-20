package com.odde.donut.controllers;

import static com.odde.donut.services.notebookExport.PortableTreeEntry.ofText;
import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Verifies that resetting a notebook's Git history restarts it from the notebook's current content
 * - including the root files it currently holds - and that only someone who can edit the notebook
 * may do so.
 */
class NotebookGitHistoryResetControllerTest extends NotebookGitBundleControllerTestBase {

  private static final String ACCEPTED_CONTENT = "---\ntype: Note\n---\naccepted content";
  private static final String OUTSIDE_HISTORY_CONTENT =
      "---\ntype: Note\n---\ncontent outside accepted history";
  private static final String EDITED_CONTENT = "---\ntype: Note\n---\nedited content";
  private static final String OVERVIEW_CONTENT = "---\ntype: Note\n---\nsee Diagram.png";
  private static final String REFERENCE_JSON = "{\"schema\": \"donut\"}\n";
  // Deliberately not valid UTF-8, so nothing on the reset path may decode these bytes.
  private static final byte[] DIAGRAM_BYTES = {(byte) 0x89, (byte) 0xFF, (byte) 0xFE, 0x00};

  @Test
  void resetRestartsHistoryFromTheCurrentNotebookSoAPlainEditPublishesAgain() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note accepted =
        makeMe.aNote().notebook(notebook).title("note").content(ACCEPTED_CONTENT).please();
    NotebookGitBinding driftedBinding = snapshotCurrentPortableTree(notebook);
    makeMe.aNote().notebook(notebook).title("outside").content(OUTSIDE_HISTORY_CONTENT).please();
    ResponseStatusException beforeReset =
        assertProposalRejectedWithoutMutatingBinding(
            notebook,
            driftedBinding.getAcceptedGitObjectId(),
            proposalBundleBytes(
                driftedBinding, List.of(new NotebookGitProposalFile("note.md", EDITED_CONTENT))),
            HttpStatus.CONFLICT);
    assertThat(beforeReset.getReason(), containsString("refresh the checkout before publishing"));

    controller.resetNotebookGitHistory(notebook);

    NotebookGitBinding afterReset = reloadCommittedBinding(notebook.getId());
    assertThat(
        afterReset.getAcceptedGitObjectId(), not(equalTo(driftedBinding.getAcceptedGitObjectId())));
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription());
        RevWalk revWalk = new RevWalk(repository)) {
      ObjectId head = GitBundleTestReader.fetchHead(repository, afterReset.getBundleBytes());
      RevCommit resetCommit = revWalk.parseCommit(head);
      assertThat(resetCommit.getParentCount(), equalTo(0));
      revWalk.markStart(resetCommit);
      List<RevCommit> history = new ArrayList<>();
      revWalk.forEach(history::add);
      assertThat(history, hasSize(1));
      assertThat(
          GitBundleTestReader.readTreeEntries(repository, resetCommit),
          contains(
              ofText("note.md", ACCEPTED_CONTENT), ofText("outside.md", OUTSIDE_HISTORY_CONTENT)));
    }

    controller.publishNotebookGitProposal(
        notebook.getId(),
        afterReset.getAcceptedGitObjectId(),
        proposalBundleBytes(
            afterReset,
            List.of(
                new NotebookGitProposalFile("note.md", EDITED_CONTENT),
                new NotebookGitProposalFile("outside.md", OUTSIDE_HISTORY_CONTENT))));

    assertThat(
        inCommittedTransaction(
            transactionManager,
            () -> noteRepository.findById(accepted.getId()).orElseThrow().getContent()),
        equalTo(EDITED_CONTENT));
  }

  @Test
  void deniedResetLeavesAcceptedHistoryUnchanged() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe.aNote().notebook(notebook).title("note").content(ACCEPTED_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    String acceptedHead = binding.getAcceptedGitObjectId();
    byte[] acceptedBundle = binding.getBundleBytes().clone();
    currentUser.setUser(createFixtureUser());

    assertThrows(
        UnexpectedNoAccessRightException.class, () -> controller.resetNotebookGitHistory(notebook));

    NotebookGitBinding after = reloadCommittedBinding(notebook.getId());
    assertThat(after.getAcceptedGitObjectId(), equalTo(acceptedHead));
    assertThat(after.getBundleBytes(), equalTo(acceptedBundle));
  }

  @Test
  void resetRestartsHistoryCarryingTheRootFilesTheNotebookCurrentlyHolds() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe.aNote("Overview").notebook(notebook).content(OVERVIEW_CONTENT).please();
    NotebookGitBinding markdownOnly = snapshotCurrentPortableTree(notebook);
    List<PortableTreeEntry> tipWithRootFiles =
        new ArrayList<>(GitBundleTestReader.fetchTipTreeEntries(markdownOnly.getBundleBytes()));
    tipWithRootFiles.add(new PortableTreeEntry("Diagram.png", DIAGRAM_BYTES));
    tipWithRootFiles.add(ofText("reference.json", REFERENCE_JSON));
    controller.publishNotebookGitProposal(
        notebook.getId(),
        markdownOnly.getAcceptedGitObjectId(),
        proposalBundleBytes(markdownOnly, NotebookGitProposalFile.asProposal(tipWithRootFiles)));

    controller.resetNotebookGitHistory(notebookRepository.findById(notebook.getId()).orElseThrow());

    NotebookGitBinding afterReset = reloadCommittedBinding(notebook.getId());
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription());
        RevWalk revWalk = new RevWalk(repository)) {
      RevCommit resetCommit =
          revWalk.parseCommit(
              GitBundleTestReader.fetchHead(repository, afterReset.getBundleBytes()));
      assertThat(resetCommit.getParentCount(), equalTo(0));
      assertThat(
          GitBundleTestReader.readTreeEntries(repository, resetCommit),
          contains(
              new PortableTreeEntry("Diagram.png", DIAGRAM_BYTES),
              ofText("Overview.md", OVERVIEW_CONTENT),
              ofText("reference.json", REFERENCE_JSON)));
    }
  }
}
