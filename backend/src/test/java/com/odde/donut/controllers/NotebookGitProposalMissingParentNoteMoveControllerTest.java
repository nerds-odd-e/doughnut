package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Publishes an ordinary-note move whose destination ancestry is absent. Identity stays on the
 * original note; parents come from admitted destination construction without dummy Readme content.
 */
class NotebookGitProposalMissingParentNoteMoveControllerTest
    extends NotebookGitWebContentControllerTestBase {

  static final String CELLS_BODY = "---\ntype: Note\n---\ncells body";

  @Test
  void publishesAMoveIntoMissingTrashAncestryCreatingParentsAndRetainingTheNote() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder biology = makeMe.aFolder().notebook(notebook).name("Biology").please();
    Note cells = makeMe.aNote("Cells").folder(biology).content(CELLS_BODY).please();
    MemoryTracker tracker = learnedTracker(cells, 0.5f, 1);
    snapshotCurrentPortableTree(notebook);
    NotebookGitBinding binding = binding(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("_trash/Biology/Cells.md", CELLS_BODY),
                new NotebookGitProposalFile("Biology/.keep", "")));
    GitBundleTestReader.SingleParentGitCommit proposedCommit = proposedCommit(proposalBytes);

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    inCommittedTransaction(
        transactionManager,
        () -> {
          Note relocated = noteRepository.findById(cells.getId()).orElseThrow();
          assertThat(relocated.isTrashed(), is(true));
          assertThat(relocated.isAvailable(), is(false));
          assertThat(relocated.getContent(), equalTo(CELLS_BODY));
          MemoryTracker learned = memoryTrackerRepository.findById(tracker.getId()).orElseThrow();
          assertThat(learned.getNote().getId(), equalTo(cells.getId()));
          assertThat(learned.isActive(), is(false));
        });
    NoteRealm shown = noteController.showNote(noteRepository.findById(cells.getId()).orElseThrow());
    assertThat(
        shown.getAncestorFolders().stream().map(Folder::getName).toList(),
        contains("_trash", "Biology"));
    assertExactAcceptedTree(notebook, proposedCommit, publishedHead, binding);
  }

  @Test
  void publishesARecoveryIntoMissingActiveAncestryWithoutDummyContent() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder biology = makeMe.aFolder().notebook(notebook).name("Biology").please();
    Note cells = makeMe.aNote("Cells").folder(biology).content(CELLS_BODY).please();
    snapshotCurrentPortableTree(notebook);
    NotebookGitBinding binding = binding(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Research/Cells.md", CELLS_BODY),
                new NotebookGitProposalFile("Biology/.keep", "")));
    GitBundleTestReader.SingleParentGitCommit proposedCommit = proposedCommit(proposalBytes);

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    Note recovered = noteRepository.findById(cells.getId()).orElseThrow();
    assertThat(recovered.getId(), equalTo(cells.getId()));
    assertThat(recovered.isTrashed(), is(false));
    assertThat(recovered.isAvailable(), is(true));
    assertThat(recovered.getTitle(), equalTo("Cells"));
    NoteRealm shown = noteController.showNote(recovered);
    assertThat(
        shown.getAncestorFolders().stream().map(Folder::getName).toList(), contains("Research"));
    assertExactAcceptedTree(notebook, proposedCommit, publishedHead, binding);
  }

  private static GitBundleTestReader.SingleParentGitCommit proposedCommit(byte[] proposalBytes)
      throws Exception {
    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription())) {
      return GitBundleTestReader.fetchSingleParentCommit(proposal, proposalBytes);
    }
  }

  private void assertExactAcceptedTree(
      Notebook notebook,
      GitBundleTestReader.SingleParentGitCommit proposedCommit,
      String publishedHead,
      NotebookGitBinding binding)
      throws Exception {
    assertThat(publishedHead, equalTo(proposedCommit.head().getName()));
    Notebook acceptedNotebook = notebookRepository.findById(notebook.getId()).orElseThrow();
    ResponseEntity<byte[]> downloaded = controller.downloadNotebookGitBundle(acceptedNotebook);
    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit downloadedCommit =
          GitBundleTestReader.fetchSingleParentCommit(readBack, downloaded.getBody());
      assertThat(downloadedCommit.head(), equalTo(proposedCommit.head()));
      assertThat(downloadedCommit.tree(), equalTo(proposedCommit.tree()));
      assertThat(downloadedCommit.parent().getName(), equalTo(binding.getAcceptedGitObjectId()));
    }
  }
}
