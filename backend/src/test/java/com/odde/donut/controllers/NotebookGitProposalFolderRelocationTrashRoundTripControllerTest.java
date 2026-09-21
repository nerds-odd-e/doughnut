package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.services.notebookTree.PortableTreeReadmeMarkdown;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

/**
 * Publishes an exact folder-subtree move into an existing {@code _trash} parent and back. Source
 * identities stay on the original folders and notes; eligibility follows the final ancestry.
 * Unrepresented empty descendants remain refused in {@link
 * NotebookGitProposalFolderRelocationEmptyDescendantControllerTest}.
 */
class NotebookGitProposalFolderRelocationTrashRoundTripControllerTest
    extends NotebookGitWebContentControllerTestBase {

  static final String README_BODY = "Biology readme";
  static final String README = PortableTreeReadmeMarkdown.assemble(README_BODY);
  static final String CELLS_BODY = "---\ntype: Note\n---\ncells body";
  static final String NUCLEUS_BODY = "---\ntype: Note\n---\nnucleus body";

  @Autowired FolderRepository folderRepository;

  @Test
  void publishesASubtreeTrashRoundTripRetainingIdentitiesEligibilityAndCanonicalKeep()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder trash = makeMe.aFolder().notebook(notebook).name("_trash").please();
    Folder biology =
        makeMe.aFolder().notebook(notebook).name("Biology").readmeContent(README_BODY).please();
    Folder empty = makeMe.aFolder().parentFolder(biology).name("Empty").please();
    Note cells = makeMe.aNote("Cells").folder(biology).content(CELLS_BODY).please();
    Note nucleus = makeMe.aNote("Nucleus").folder(biology).content(NUCLEUS_BODY).please();
    MemoryTracker tracker = learnedTracker(cells, 0.5f, 1);
    snapshotCurrentPortableTree(notebook);
    NotebookGitBinding beforeTrash = binding(notebook);
    byte[] trashProposal =
        proposalBundleBytes(
            beforeTrash,
            List.of(
                new NotebookGitProposalFile("_trash/Biology/README.md", README),
                new NotebookGitProposalFile("_trash/Biology/Cells.md", CELLS_BODY),
                new NotebookGitProposalFile("_trash/Biology/Nucleus.md", NUCLEUS_BODY),
                new NotebookGitProposalFile("_trash/Biology/Empty/.keep", "")));
    GitBundleTestReader.SingleParentGitCommit trashCommit = proposedCommit(trashProposal);

    String trashHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), beforeTrash.getAcceptedGitObjectId(), trashProposal);

    inCommittedTransaction(
        transactionManager,
        () -> {
          Folder relocatedBiology = folderRepository.findById(biology.getId()).orElseThrow();
          Folder relocatedEmpty = folderRepository.findById(empty.getId()).orElseThrow();
          Folder existingTrash = folderRepository.findById(trash.getId()).orElseThrow();
          assertThat(
              folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()).stream()
                  .map(Folder::getId)
                  .toList(),
              containsInAnyOrder(trash.getId(), biology.getId(), empty.getId()));
          assertThat(existingTrash.getParentFolderId(), nullValue());
          assertThat(relocatedBiology.getParentFolderId(), equalTo(trash.getId()));
          assertThat(relocatedEmpty.getParentFolderId(), equalTo(biology.getId()));
          assertThat(relocatedBiology.isTrashed(), is(true));
          assertThat(relocatedEmpty.isTrashed(), is(true));
          Note relocatedCells = noteRepository.findById(cells.getId()).orElseThrow();
          Note relocatedNucleus = noteRepository.findById(nucleus.getId()).orElseThrow();
          assertThat(relocatedCells.getFolder().getId(), equalTo(biology.getId()));
          assertThat(relocatedNucleus.getFolder().getId(), equalTo(biology.getId()));
          assertThat(relocatedCells.getContent(), equalTo(CELLS_BODY));
          assertThat(relocatedNucleus.getContent(), equalTo(NUCLEUS_BODY));
          assertThat(relocatedCells.isAvailable(), is(false));
          assertThat(relocatedNucleus.isAvailable(), is(false));
          MemoryTracker learned = memoryTrackerRepository.findById(tracker.getId()).orElseThrow();
          assertThat(learned.getNote().getId(), equalTo(cells.getId()));
          assertThat(learned.isActive(), is(false));
        });
    assertExactAcceptedTree(
        notebook,
        trashCommit,
        trashHead,
        beforeTrash,
        List.of(
            "_trash/Biology/README.md",
            "_trash/Biology/Cells.md",
            "_trash/Biology/Nucleus.md",
            "_trash/Biology/Empty/.keep"),
        "_trash/Biology/README.md",
        README,
        "_trash/Biology/Empty/.keep",
        "");

    NotebookGitBinding beforeRecovery = binding(notebook);
    byte[] recoveryProposal =
        proposalBundleBytes(
            beforeRecovery,
            List.of(
                new NotebookGitProposalFile("Biology/README.md", README),
                new NotebookGitProposalFile("Biology/Cells.md", CELLS_BODY),
                new NotebookGitProposalFile("Biology/Nucleus.md", NUCLEUS_BODY),
                new NotebookGitProposalFile("Biology/Empty/.keep", ""),
                new NotebookGitProposalFile("_trash/.keep", "")));
    GitBundleTestReader.SingleParentGitCommit recoveryCommit = proposedCommit(recoveryProposal);

    String recoveredHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), beforeRecovery.getAcceptedGitObjectId(), recoveryProposal);

    inCommittedTransaction(
        transactionManager,
        () -> {
          Folder recoveredBiology = folderRepository.findById(biology.getId()).orElseThrow();
          Folder recoveredEmpty = folderRepository.findById(empty.getId()).orElseThrow();
          assertThat(
              folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()).stream()
                  .map(Folder::getId)
                  .toList(),
              containsInAnyOrder(trash.getId(), biology.getId(), empty.getId()));
          assertThat(recoveredBiology.getParentFolderId(), nullValue());
          assertThat(recoveredEmpty.getParentFolderId(), equalTo(biology.getId()));
          assertThat(recoveredBiology.isTrashed(), is(false));
          Note recoveredCells = noteRepository.findById(cells.getId()).orElseThrow();
          Note recoveredNucleus = noteRepository.findById(nucleus.getId()).orElseThrow();
          assertThat(recoveredCells.getContent(), equalTo(CELLS_BODY));
          assertThat(recoveredNucleus.getContent(), equalTo(NUCLEUS_BODY));
          assertThat(recoveredCells.isAvailable(), is(true));
          assertThat(recoveredNucleus.isAvailable(), is(true));
          MemoryTracker learned = memoryTrackerRepository.findById(tracker.getId()).orElseThrow();
          assertThat(learned.getNote().getId(), equalTo(cells.getId()));
          assertThat(learned.isActive(), is(true));
        });
    assertExactAcceptedTree(
        notebook,
        recoveryCommit,
        recoveredHead,
        beforeRecovery,
        List.of(
            "Biology/README.md",
            "Biology/Cells.md",
            "Biology/Nucleus.md",
            "Biology/Empty/.keep",
            "_trash/.keep"),
        "Biology/Cells.md",
        CELLS_BODY,
        "Biology/Empty/.keep",
        "");
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
      NotebookGitBinding parentBinding,
      List<String> expectedPaths,
      String samplePath,
      String sampleBytes,
      String keepPath,
      String keepBytes)
      throws Exception {
    assertThat(publishedHead, equalTo(proposedCommit.head().getName()));
    Notebook acceptedNotebook = notebookRepository.findById(notebook.getId()).orElseThrow();
    ResponseEntity<byte[]> downloaded = controller.downloadNotebookGitBundle(acceptedNotebook);
    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit downloadedCommit =
          GitBundleTestReader.fetchSingleParentCommit(readBack, downloaded.getBody());
      assertThat(downloadedCommit.head(), equalTo(proposedCommit.head()));
      assertThat(downloadedCommit.tree(), equalTo(proposedCommit.tree()));
      assertThat(
          downloadedCommit.parent().getName(), equalTo(parentBinding.getAcceptedGitObjectId()));
      assertThat(
          GitBundleTestReader.pathsIn(readBack, downloadedCommit.head()),
          containsInAnyOrder(expectedPaths.toArray(String[]::new)));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(readBack, downloadedCommit.head(), samplePath),
          equalTo(sampleBytes));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(readBack, downloadedCommit.head(), keepPath),
          equalTo(keepBytes));
    }
  }
}
