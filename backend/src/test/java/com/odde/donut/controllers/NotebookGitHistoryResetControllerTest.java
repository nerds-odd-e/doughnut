package com.odde.donut.controllers;

import static com.odde.donut.services.notebookAttachment.VerifiedNotebookAttachmentBytes.sha256Hex;
import static com.odde.donut.services.notebookTree.PortableTreeEntry.ofText;
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
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NotebookAttachmentRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookGit.NotebookGitAttributes;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Verifies that resetting a notebook's Git history restarts it from the notebook's current content
 * - including the root files and authored Git metadata it currently holds - and that only someone
 * who can edit the notebook may do so.
 */
class NotebookGitHistoryResetControllerTest extends NotebookGitControllerTestBase {

  private static final String ACCEPTED_CONTENT = "---\ntype: Note\n---\naccepted content";
  private static final String OUTSIDE_HISTORY_CONTENT =
      "---\ntype: Note\n---\ncontent outside accepted history";
  private static final String EDITED_CONTENT = "---\ntype: Note\n---\nedited content";
  private static final String OVERVIEW_CONTENT = "---\ntype: Note\n---\nsee Diagram.png";
  private static final String REFERENCE_JSON = "{\"schema\": \"donut\"}\n";
  private static final String AUTHORED_ATTRIBUTES = "* filter=lfs -text\nauthored !filter\n";
  // Deliberately not valid UTF-8, so nothing on the reset path may decode these bytes.
  private static final byte[] DIAGRAM_BYTES = {(byte) 0x89, (byte) 0xFF, (byte) 0xFE, 0x00};

  @Autowired NotebookAttachmentRepository notebookAttachmentRepository;

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

    byte[] downloadedAfterReset =
        controller
            .downloadNotebookGitBundle(notebookRepository.findById(notebook.getId()).orElseThrow())
            .getBody();
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription());
        RevWalk revWalk = new RevWalk(repository)) {
      ObjectId head = GitBundleTestReader.fetchHead(repository, downloadedAfterReset);
      assertThat(head, equalTo(ObjectId.fromString(afterReset.getAcceptedGitObjectId())));
      RevCommit resetCommit = revWalk.parseCommit(head);
      assertThat(resetCommit.getParentCount(), equalTo(0));
      revWalk.markStart(resetCommit);
      List<RevCommit> history = new ArrayList<>();
      revWalk.forEach(history::add);
      assertThat(history, hasSize(1));
      assertThat(
          GitBundleTestReader.readContent(repository, resetCommit),
          contains(
              ofText("note.md", ACCEPTED_CONTENT), ofText("outside.md", OUTSIDE_HISTORY_CONTENT)));

      // The reset's new root commit is disjoint (no parents): confirm the pre-reset head is
      // genuinely unreachable/not advertised, not merely absent from the visited history list.
      ObjectId driftedHead = ObjectId.fromString(driftedBinding.getAcceptedGitObjectId());
      assertThrows(MissingObjectException.class, () -> revWalk.parseCommit(driftedHead));
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
  void resetOfANotebookWithoutBindingCreatesABindingWithInitialLfsAttributes() throws Exception {
    Notebook notebook =
        inCommittedTransaction(
            transactionManager,
            () -> makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please());

    controller.resetNotebookGitHistory(notebook);

    assertThat(
        acceptedHistory(notebook).exactTree(),
        contains(ofText(NotebookGitAttributes.PATH, NotebookGitAttributes.INITIAL_CONTENT)));
  }

  @Test
  void deniedResetLeavesAcceptedHistoryUnchanged() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe.aNote().notebook(notebook).title("note").content(ACCEPTED_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    String acceptedHead = binding.getAcceptedGitObjectId();
    var acceptedHistoryBefore = acceptedHistory(notebook);
    User owner = currentUser.getUser();
    currentUser.setUser(createFixtureUser());

    assertThrows(
        UnexpectedNoAccessRightException.class, () -> controller.resetNotebookGitHistory(notebook));

    currentUser.setUser(owner);
    NotebookGitBinding after = reloadCommittedBinding(notebook.getId());
    assertThat(after.getAcceptedGitObjectId(), equalTo(acceptedHead));
    assertThat(acceptedHistory(notebook), equalTo(acceptedHistoryBefore));
  }

  @Test
  void resetRestartsHistoryCarryingTheRootFilesTheNotebookCurrentlyHolds() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe.aNote("Overview").notebook(notebook).content(OVERVIEW_CONTENT).please();
    NotebookGitBinding markdownOnly = snapshotCurrentPortableTree(notebook);
    List<PortableTreeEntry> current =
        List.of(
            ofText(NotebookGitAttributes.PATH, AUTHORED_ATTRIBUTES),
            new PortableTreeEntry("Diagram.png", DIAGRAM_BYTES),
            ofText("Overview.md", OVERVIEW_CONTENT),
            ofText("reference.json", REFERENCE_JSON));
    List<PortableTreeEntry> tip = asCommitted(notebook, current);
    controller.publishNotebookGitProposal(
        notebook.getId(),
        markdownOnly.getAcceptedGitObjectId(),
        proposalBundleBytes(markdownOnly, NotebookGitProposalFile.asProposal(tip)));

    controller.resetNotebookGitHistory(notebookRepository.findById(notebook.getId()).orElseThrow());

    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription());
        RevWalk revWalk = new RevWalk(repository)) {
      RevCommit resetCommit =
          revWalk.parseCommit(
              GitBundleTestReader.fetchHead(repository, acceptedBundleBytes(notebook)));
      assertThat(resetCommit.getParentCount(), equalTo(0));
      assertThat(GitBundleTestReader.readExactTree(repository, resetCommit), equalTo(tip));
    }
    for (PortableTreeEntry file :
        current.stream().filter(NotebookGitControllerTestBase::isAttachment).toList()) {
      assertThat(
          notebookAttachmentContent.get(notebook.getId(), sha256Hex(file.content())).orElseThrow(),
          equalTo(file.content()));
    }
    assertThat(
        NotebookLiveProjectionTestReader.rootAttachments(
            transactionManager, notebookAttachmentRepository, notebook.getId()),
        equalTo(tip.stream().filter(NotebookGitControllerTestBase::isAttachment).toList()));
  }
}
