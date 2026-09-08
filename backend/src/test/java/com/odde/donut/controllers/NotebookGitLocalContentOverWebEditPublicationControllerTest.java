package com.odde.donut.controllers;

import static com.odde.donut.controllers.NotebookGitNoteCreationControllerTestSupport.titleOnly;
import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

/**
 * Verifies that a one-child proposal of chosen local content, parented on an accepted web edit or
 * web creation, is accepted on the original learned identities — both when the web change is a
 * different note and when it is the same learned note.
 */
class NotebookGitLocalContentOverWebEditPublicationControllerTest
    extends NotebookGitWebContentControllerTestBase {

  private static final String LOCAL_NOTE_PATH = "Local Note.md";
  private static final String WEB_NOTE_PATH = "Web Note.md";
  private static final String LEARNED_NOTE_PATH = "Learned Note.md";
  private static final String ALPHA_PATH = "Alpha.md";
  private static final String BETA_PATH = "Beta.md";
  private static final String ORIGINAL_CONTENT = "---\ntype: Note\n---\noriginal content";
  private static final String LOCAL_CONTENT = "---\ntype: Note\n---\nlocal patch";
  private static final String WEB_CONTENT = "---\ntype: Note\n---\nweb edit";
  private static final String CHOSEN_CONTENT =
      "---\ntype: Note\nauthored: chosen\n---\nchosen resolution";

  @Autowired MemoryTrackerRepository memoryTrackerRepository;

  @Test
  void publishesLocalContentOfOneNoteOntoAnAcceptedWebEditOfAnother() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note localNote =
        makeMe.aNote().notebook(notebook).title("Local Note").content(ORIGINAL_CONTENT).please();
    Note webNote =
        makeMe.aNote().notebook(notebook).title("Web Note").content(ORIGINAL_CONTENT).please();
    MemoryTracker tracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(localNote.getId()).orElseThrow())
                    .difficulty(7f)
                    .please());
    snapshotCurrentPortableTree(notebook);

    textContentController.updateNoteContent(webNote, contentDto(WEB_CONTENT));
    NotebookGitBinding afterWebSave = binding(notebook);
    ObjectId webSaveHead = ObjectId.fromString(afterWebSave.getAcceptedGitObjectId());
    byte[] proposalBytes =
        proposalBundleBytes(
            afterWebSave,
            List.of(
                new NotebookGitProposalFile(LOCAL_NOTE_PATH, LOCAL_CONTENT),
                new NotebookGitProposalFile(WEB_NOTE_PATH, WEB_CONTENT)));

    GitBundleTestReader.SingleParentGitCommit proposedCommit;
    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription())) {
      proposedCommit = GitBundleTestReader.fetchSingleParentCommit(proposal, proposalBytes);
      assertThat(proposedCommit.parent(), equalTo(webSaveHead));
    }

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), afterWebSave.getAcceptedGitObjectId(), proposalBytes);

    Note reloadedLocal = noteRepository.findById(localNote.getId()).orElseThrow();
    Note reloadedWeb = noteRepository.findById(webNote.getId()).orElseThrow();
    NoteRecallInfo recallInfo = noteController.getNoteInfo(reloadedLocal);
    assertThat(reloadedLocal.getId(), equalTo(localNote.getId()));
    assertThat(reloadedLocal.getContent(), equalTo(LOCAL_CONTENT));
    assertThat(recallInfo.getMemoryTrackers(), hasSize(1));
    assertThat(recallInfo.getMemoryTrackers().getFirst().getId(), equalTo(tracker.getId()));
    assertThat(recallInfo.getMemoryTrackers().getFirst().getDifficulty(), equalTo(7f));
    assertThat(reloadedWeb.getId(), equalTo(webNote.getId()));
    assertThat(reloadedWeb.getContent(), equalTo(WEB_CONTENT));
    assertThat(publishedHead, equalTo(proposedCommit.head().getName()));

    Notebook acceptedNotebook = notebookRepository.findById(notebook.getId()).orElseThrow();
    ResponseEntity<byte[]> downloaded = controller.downloadNotebookGitBundle(acceptedNotebook);
    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit downloadedCommit =
          GitBundleTestReader.fetchSingleParentCommit(readBack, downloaded.getBody());
      assertThat(downloadedCommit.head(), equalTo(proposedCommit.head()));
      assertThat(downloadedCommit.tree(), equalTo(proposedCommit.tree()));
      assertThat(downloadedCommit.parent(), equalTo(webSaveHead));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(readBack, downloadedCommit.head(), LOCAL_NOTE_PATH),
          equalTo(LOCAL_CONTENT));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(readBack, downloadedCommit.head(), WEB_NOTE_PATH),
          equalTo(WEB_CONTENT));
    }
  }

  @Test
  void publishesLocalContentOfOneNoteOntoAnAcceptedWebCreationOfAnother() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note alpha =
        makeMe.aNote().notebook(notebook).title("Alpha").content(ORIGINAL_CONTENT).please();
    snapshotCurrentPortableTree(notebook);

    NoteRealm betaRealm = controller.createNoteAtNotebookRoot(notebook, titleOnly("Beta"));
    Integer betaId = betaRealm.getId();
    NotebookGitBinding afterCreation = binding(notebook);
    ObjectId creationHead = ObjectId.fromString(afterCreation.getAcceptedGitObjectId());
    String betaContent = noteRepository.findById(betaId).orElseThrow().getContent();

    MemoryTracker alphaTracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(alpha.getId()).orElseThrow())
                    .difficulty(7f)
                    .recallCount(1)
                    .please());
    MemoryTracker betaTracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(betaId).orElseThrow())
                    .difficulty(5f)
                    .recallCount(2)
                    .please());

    assertThat(
        noteRepository.findById(alpha.getId()).orElseThrow().getContent(),
        equalTo(ORIGINAL_CONTENT));

    byte[] proposalBytes =
        proposalBundleBytes(
            afterCreation,
            List.of(
                new NotebookGitProposalFile(ALPHA_PATH, LOCAL_CONTENT),
                new NotebookGitProposalFile(BETA_PATH, betaContent)));
    GitBundleTestReader.SingleParentGitCommit proposedCommit;
    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription())) {
      proposedCommit = GitBundleTestReader.fetchSingleParentCommit(proposal, proposalBytes);
      assertThat(proposedCommit.parent(), equalTo(creationHead));
    }

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), afterCreation.getAcceptedGitObjectId(), proposalBytes);

    Note reloadedAlpha = noteRepository.findById(alpha.getId()).orElseThrow();
    Note reloadedBeta = noteRepository.findById(betaId).orElseThrow();
    NoteRecallInfo alphaRecall = noteController.getNoteInfo(reloadedAlpha);
    NoteRecallInfo betaRecall = noteController.getNoteInfo(reloadedBeta);
    MemoryTracker retainedAlpha =
        memoryTrackerRepository.findById(alphaTracker.getId()).orElseThrow();
    MemoryTracker retainedBeta =
        memoryTrackerRepository.findById(betaTracker.getId()).orElseThrow();

    assertThat(reloadedAlpha.getId(), equalTo(alpha.getId()));
    assertThat(reloadedAlpha.getContent(), equalTo(LOCAL_CONTENT));
    assertThat(alphaRecall.getMemoryTrackers(), hasSize(1));
    assertThat(alphaRecall.getMemoryTrackers().getFirst().getId(), equalTo(alphaTracker.getId()));
    assertThat(retainedAlpha.getDifficulty(), equalTo(7f));
    assertThat(retainedAlpha.getStability(), equalTo(alphaTracker.getStability()));
    assertThat(retainedAlpha.getLastRecalledAt(), equalTo(alphaTracker.getLastRecalledAt()));
    assertThat(retainedAlpha.getNextRecallAt(), equalTo(alphaTracker.getNextRecallAt()));
    assertThat(retainedAlpha.getAssimilatedAt(), equalTo(alphaTracker.getAssimilatedAt()));
    assertThat(retainedAlpha.getRecallCount(), equalTo(1));

    assertThat(reloadedBeta.getId(), equalTo(betaId));
    assertThat(reloadedBeta.getContent(), equalTo(betaContent));
    assertThat(betaRecall.getMemoryTrackers(), hasSize(1));
    assertThat(betaRecall.getMemoryTrackers().getFirst().getId(), equalTo(betaTracker.getId()));
    assertThat(retainedBeta.getDifficulty(), equalTo(5f));
    assertThat(retainedBeta.getStability(), equalTo(betaTracker.getStability()));
    assertThat(retainedBeta.getLastRecalledAt(), equalTo(betaTracker.getLastRecalledAt()));
    assertThat(retainedBeta.getNextRecallAt(), equalTo(betaTracker.getNextRecallAt()));
    assertThat(retainedBeta.getAssimilatedAt(), equalTo(betaTracker.getAssimilatedAt()));
    assertThat(retainedBeta.getRecallCount(), equalTo(2));
    assertThat(publishedHead, equalTo(proposedCommit.head().getName()));

    Notebook acceptedNotebook = notebookRepository.findById(notebook.getId()).orElseThrow();
    ResponseEntity<byte[]> downloaded = controller.downloadNotebookGitBundle(acceptedNotebook);
    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit downloadedCommit =
          GitBundleTestReader.fetchSingleParentCommit(readBack, downloaded.getBody());
      assertThat(downloadedCommit.head(), equalTo(proposedCommit.head()));
      assertThat(downloadedCommit.tree(), equalTo(proposedCommit.tree()));
      assertThat(downloadedCommit.parent(), equalTo(creationHead));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(readBack, downloadedCommit.head(), ALPHA_PATH),
          equalTo(LOCAL_CONTENT));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(readBack, downloadedCommit.head(), BETA_PATH),
          equalTo(betaContent));
    }
  }

  @Test
  void publishesChosenContentOnTheSameLearnedNoteAfterAnAcceptedWebSave() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note learned =
        makeMe.aNote().notebook(notebook).title("Learned Note").content(ORIGINAL_CONTENT).please();
    MemoryTracker tracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(learned.getId()).orElseThrow())
                    .difficulty(7f)
                    .please());
    snapshotCurrentPortableTree(notebook);

    textContentController.updateNoteContent(learned, contentDto(WEB_CONTENT));
    NotebookGitBinding afterWebSave = binding(notebook);
    ObjectId webSaveHead = ObjectId.fromString(afterWebSave.getAcceptedGitObjectId());
    byte[] proposalBytes =
        proposalBundleBytes(
            afterWebSave, List.of(new NotebookGitProposalFile(LEARNED_NOTE_PATH, CHOSEN_CONTENT)));

    GitBundleTestReader.SingleParentGitCommit proposedCommit;
    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription())) {
      proposedCommit = GitBundleTestReader.fetchSingleParentCommit(proposal, proposalBytes);
      assertThat(proposedCommit.parent(), equalTo(webSaveHead));
    }

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), afterWebSave.getAcceptedGitObjectId(), proposalBytes);

    Note reloaded = noteRepository.findById(learned.getId()).orElseThrow();
    assertThat(reloaded.getId(), equalTo(learned.getId()));
    assertThat(reloaded.getContent(), equalTo(CHOSEN_CONTENT));
    assertThat(
        noteController.getNoteInfo(reloaded).getMemoryTrackers().getFirst().getId(),
        equalTo(tracker.getId()));
    assertThat(publishedHead, equalTo(proposedCommit.head().getName()));

    Notebook acceptedNotebook = notebookRepository.findById(notebook.getId()).orElseThrow();
    ResponseEntity<byte[]> downloaded = controller.downloadNotebookGitBundle(acceptedNotebook);
    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit downloadedCommit =
          GitBundleTestReader.fetchSingleParentCommit(readBack, downloaded.getBody());
      assertThat(downloadedCommit.head(), equalTo(proposedCommit.head()));
      assertThat(downloadedCommit.tree(), equalTo(proposedCommit.tree()));
      assertThat(downloadedCommit.parent(), equalTo(webSaveHead));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(
              readBack, downloadedCommit.head(), LEARNED_NOTE_PATH),
          equalTo(CHOSEN_CONTENT));
    }
  }
}
