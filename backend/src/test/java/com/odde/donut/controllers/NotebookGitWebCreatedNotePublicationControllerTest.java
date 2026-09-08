package com.odde.donut.controllers;

import static com.odde.donut.controllers.NotebookGitNoteCreationControllerTestSupport.titleOnly;
import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.controllers.dto.NoteRecallInfo;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

/**
 * Verifies that a local content proposal publishes onto a real web-created note after a later
 * ordinary web body save, retaining that identity and its learning state.
 */
class NotebookGitWebCreatedNotePublicationControllerTest
    extends NotebookGitWebContentControllerTestBase {

  private static final String EXISTING_PATH = "Existing.md";
  private static final String CREATED_PATH = "Created.md";
  private static final String EXISTING_CONTENT = "---\ntype: Note\n---\nexisting content";
  private static final String WEB_CONTENT = "---\ntype: Note\n---\nweb body";
  private static final String LOCAL_CONTENT = "---\ntype: Note\n---\nlocal refinement";

  @Autowired MemoryTrackerRepository memoryTrackerRepository;

  @Test
  void publishesLocalContentOntoTheSameWebCreatedNoteAfterAnAcceptedWebSave() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note existing =
        makeMe.aNote().notebook(notebook).title("Existing").content(EXISTING_CONTENT).please();
    snapshotCurrentPortableTree(notebook);

    NoteRealm createdRealm = controller.createNoteAtNotebookRoot(notebook, titleOnly("Created"));
    Integer createdId = createdRealm.getId();
    ObjectId creationHead = ObjectId.fromString(binding(notebook).getAcceptedGitObjectId());
    MemoryTracker tracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(createdId).orElseThrow())
                    .difficulty(7f)
                    .recallCount(1)
                    .please());

    textContentController.updateNoteContent(
        noteRepository.findById(createdId).orElseThrow(), contentDto(WEB_CONTENT));
    NotebookGitBinding afterWebSave = binding(notebook);
    ObjectId webSaveHead = ObjectId.fromString(afterWebSave.getAcceptedGitObjectId());
    assertThat(webSaveHead, not(equalTo(creationHead)));

    byte[] proposalBytes =
        proposalBundleBytes(
            afterWebSave,
            List.of(
                new NotebookGitProposalFile(EXISTING_PATH, EXISTING_CONTENT),
                new NotebookGitProposalFile(CREATED_PATH, LOCAL_CONTENT)));
    GitBundleTestReader.SingleParentGitCommit proposedCommit;
    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription())) {
      proposedCommit = GitBundleTestReader.fetchSingleParentCommit(proposal, proposalBytes);
      assertThat(proposedCommit.parent(), equalTo(webSaveHead));
    }

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), afterWebSave.getAcceptedGitObjectId(), proposalBytes);

    Note reloadedCreated = noteRepository.findById(createdId).orElseThrow();
    Note reloadedExisting = noteRepository.findById(existing.getId()).orElseThrow();
    NoteRecallInfo recallInfo = noteController.getNoteInfo(reloadedCreated);
    MemoryTracker retained = memoryTrackerRepository.findById(tracker.getId()).orElseThrow();
    assertThat(reloadedCreated.getId(), equalTo(createdId));
    assertThat(reloadedCreated.getContent(), equalTo(LOCAL_CONTENT));
    assertThat(recallInfo.getMemoryTrackers(), hasSize(1));
    assertThat(recallInfo.getMemoryTrackers().getFirst().getId(), equalTo(tracker.getId()));
    assertThat(retained.getDifficulty(), equalTo(tracker.getDifficulty()));
    assertThat(retained.getStability(), equalTo(tracker.getStability()));
    assertThat(retained.getLastRecalledAt(), equalTo(tracker.getLastRecalledAt()));
    assertThat(retained.getNextRecallAt(), equalTo(tracker.getNextRecallAt()));
    assertThat(retained.getAssimilatedAt(), equalTo(tracker.getAssimilatedAt()));
    assertThat(retained.getRecallCount(), equalTo(1));
    assertThat(reloadedExisting.getId(), equalTo(existing.getId()));
    assertThat(reloadedExisting.getContent(), equalTo(EXISTING_CONTENT));
    assertThat(publishedHead, equalTo(proposedCommit.head().getName()));

    ResponseEntity<byte[]> downloaded =
        controller.downloadNotebookGitBundle(
            notebookRepository.findById(notebook.getId()).orElseThrow());
    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit downloadedCommit =
          GitBundleTestReader.fetchSingleParentCommit(readBack, downloaded.getBody());
      assertThat(downloadedCommit.head(), equalTo(proposedCommit.head()));
      assertThat(downloadedCommit.parent(), equalTo(webSaveHead));
      try (RevWalk revWalk = new RevWalk(readBack)) {
        RevCommit webSaveCommit = revWalk.parseCommit(webSaveHead);
        assertThat(webSaveCommit.getParentCount(), is(1));
        assertThat(webSaveCommit.getParent(0).getId(), equalTo(creationHead));
      }
      assertThat(
          NotebookGitProposalBlobText.readUtf8(readBack, downloadedCommit.head(), CREATED_PATH),
          equalTo(LOCAL_CONTENT));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(readBack, downloadedCommit.head(), EXISTING_PATH),
          equalTo(EXISTING_CONTENT));
    }
  }
}
