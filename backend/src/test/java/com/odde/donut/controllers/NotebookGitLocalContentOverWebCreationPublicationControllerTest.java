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
 * Verifies that a one-child proposal of chosen local content, parented on an accepted web creation
 * or web creation-then-save of a different note, is accepted on the original learned identities.
 */
class NotebookGitLocalContentOverWebCreationPublicationControllerTest
    extends NotebookGitWebContentControllerTestBase {

  private static final String ALPHA_PATH = "Alpha.md";
  private static final String BETA_PATH = "Beta.md";
  private static final String ORIGINAL_CONTENT = "---\ntype: Note\n---\noriginal content";
  private static final String LOCAL_CONTENT = "---\ntype: Note\n---\nlocal patch";
  private static final String WEB_CONTENT = "---\ntype: Note\n---\nweb edit";

  @Autowired MemoryTrackerRepository memoryTrackerRepository;

  @Test
  void publishesLocalContentOfOneNoteOntoAnAcceptedWebCreationOfAnother() throws Exception {
    NotebookWithCreatedBeta created = notebookWithWebCreatedBeta();
    AlphaBetaTrackers trackers = seedAlphaBetaTrackers(created.alpha(), created.betaId());
    NotebookGitBinding afterCreation = created.accepted();
    ObjectId creationHead = created.acceptedHead();
    String betaContent = created.betaContent();

    assertThat(
        noteRepository.findById(created.alpha().getId()).orElseThrow().getContent(),
        equalTo(ORIGINAL_CONTENT));

    PublishedProposal published =
        publishAlphaLocalOverBeta(created.notebook(), afterCreation, creationHead, betaContent);

    Note reloadedAlpha = noteRepository.findById(created.alpha().getId()).orElseThrow();
    Note reloadedBeta = noteRepository.findById(created.betaId()).orElseThrow();
    NoteRecallInfo alphaRecall = noteController.getNoteInfo(reloadedAlpha);
    NoteRecallInfo betaRecall = noteController.getNoteInfo(reloadedBeta);
    MemoryTracker retainedAlpha =
        memoryTrackerRepository.findById(trackers.alphaTracker().getId()).orElseThrow();
    MemoryTracker retainedBeta =
        memoryTrackerRepository.findById(trackers.betaTracker().getId()).orElseThrow();

    assertThat(reloadedAlpha.getId(), equalTo(created.alpha().getId()));
    assertThat(reloadedAlpha.getContent(), equalTo(LOCAL_CONTENT));
    assertThat(alphaRecall.getMemoryTrackers(), hasSize(1));
    assertThat(
        alphaRecall.getMemoryTrackers().getFirst().getId(),
        equalTo(trackers.alphaTracker().getId()));
    assertThat(retainedAlpha.getDifficulty(), equalTo(7f));
    assertThat(retainedAlpha.getStability(), equalTo(trackers.alphaTracker().getStability()));
    assertThat(
        retainedAlpha.getLastRecalledAt(), equalTo(trackers.alphaTracker().getLastRecalledAt()));
    assertThat(retainedAlpha.getNextRecallAt(), equalTo(trackers.alphaTracker().getNextRecallAt()));
    assertThat(
        retainedAlpha.getAssimilatedAt(), equalTo(trackers.alphaTracker().getAssimilatedAt()));
    assertThat(retainedAlpha.getRecallCount(), equalTo(1));

    assertThat(reloadedBeta.getId(), equalTo(created.betaId()));
    assertThat(reloadedBeta.getContent(), equalTo(betaContent));
    assertThat(betaRecall.getMemoryTrackers(), hasSize(1));
    assertThat(
        betaRecall.getMemoryTrackers().getFirst().getId(), equalTo(trackers.betaTracker().getId()));
    assertThat(retainedBeta.getDifficulty(), equalTo(5f));
    assertThat(retainedBeta.getStability(), equalTo(trackers.betaTracker().getStability()));
    assertThat(
        retainedBeta.getLastRecalledAt(), equalTo(trackers.betaTracker().getLastRecalledAt()));
    assertThat(retainedBeta.getNextRecallAt(), equalTo(trackers.betaTracker().getNextRecallAt()));
    assertThat(retainedBeta.getAssimilatedAt(), equalTo(trackers.betaTracker().getAssimilatedAt()));
    assertThat(retainedBeta.getRecallCount(), equalTo(2));
    assertThat(published.publishedHead(), equalTo(published.proposedCommit().head().getName()));

    assertDownloadedProposal(
        created.notebook(), published.proposedCommit(), creationHead, betaContent, null);
  }

  @Test
  void publishesLocalContentOfOneNoteOntoAnAcceptedWebCreationThenSaveOfAnother() throws Exception {
    NotebookWithCreatedBeta created = notebookWithWebCreatedBeta();
    ObjectId creationHead = created.acceptedHead();

    textContentController.updateNoteContent(
        noteRepository.findById(created.betaId()).orElseThrow(), contentDto(WEB_CONTENT));
    NotebookGitBinding afterSave = binding(created.notebook());
    ObjectId saveHead = ObjectId.fromString(afterSave.getAcceptedGitObjectId());
    assertThat(saveHead, not(equalTo(creationHead)));
    String betaContent = noteRepository.findById(created.betaId()).orElseThrow().getContent();
    assertThat(betaContent, equalTo(WEB_CONTENT));

    AlphaBetaTrackers trackers = seedAlphaBetaTrackers(created.alpha(), created.betaId());
    PublishedProposal published =
        publishAlphaLocalOverBeta(created.notebook(), afterSave, saveHead, betaContent);

    Note reloadedAlpha = noteRepository.findById(created.alpha().getId()).orElseThrow();
    Note reloadedBeta = noteRepository.findById(created.betaId()).orElseThrow();

    assertThat(reloadedAlpha.getId(), equalTo(created.alpha().getId()));
    assertThat(reloadedAlpha.getContent(), equalTo(LOCAL_CONTENT));
    assertThat(reloadedBeta.getId(), equalTo(created.betaId()));
    assertThat(reloadedBeta.getContent(), equalTo(WEB_CONTENT));
    assertThat(
        noteController.getNoteInfo(reloadedAlpha).getMemoryTrackers().getFirst().getId(),
        equalTo(trackers.alphaTracker().getId()));
    assertThat(
        noteController.getNoteInfo(reloadedBeta).getMemoryTrackers().getFirst().getId(),
        equalTo(trackers.betaTracker().getId()));
    assertThat(published.publishedHead(), equalTo(published.proposedCommit().head().getName()));

    assertDownloadedProposal(
        created.notebook(), published.proposedCommit(), saveHead, WEB_CONTENT, creationHead);
  }

  private NotebookWithCreatedBeta notebookWithWebCreatedBeta() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note alpha =
        makeMe.aNote().notebook(notebook).title("Alpha").content(ORIGINAL_CONTENT).please();
    snapshotCurrentPortableTree(notebook);

    NoteRealm betaRealm = controller.createNoteAtNotebookRoot(notebook, titleOnly("Beta"));
    Integer betaId = betaRealm.getId();
    NotebookGitBinding accepted = binding(notebook);
    ObjectId acceptedHead = ObjectId.fromString(accepted.getAcceptedGitObjectId());
    String betaContent = noteRepository.findById(betaId).orElseThrow().getContent();

    return new NotebookWithCreatedBeta(
        notebook, alpha, betaId, accepted, acceptedHead, betaContent);
  }

  private AlphaBetaTrackers seedAlphaBetaTrackers(Note alpha, Integer betaId) {
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
    return new AlphaBetaTrackers(alphaTracker, betaTracker);
  }

  private PublishedProposal publishAlphaLocalOverBeta(
      Notebook notebook, NotebookGitBinding accepted, ObjectId expectedParent, String betaContent)
      throws Exception {
    byte[] proposalBytes =
        proposalBundleBytes(
            accepted,
            List.of(
                new NotebookGitProposalFile(ALPHA_PATH, LOCAL_CONTENT),
                new NotebookGitProposalFile(BETA_PATH, betaContent)));
    GitBundleTestReader.SingleParentGitCommit proposedCommit;
    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription())) {
      proposedCommit = GitBundleTestReader.fetchSingleParentCommit(proposal, proposalBytes);
      assertThat(proposedCommit.parent(), equalTo(expectedParent));
    }

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), accepted.getAcceptedGitObjectId(), proposalBytes);
    return new PublishedProposal(publishedHead, proposedCommit);
  }

  private void assertDownloadedProposal(
      Notebook notebook,
      GitBundleTestReader.SingleParentGitCommit proposedCommit,
      ObjectId expectedParent,
      String betaContent,
      ObjectId expectedParentOfAcceptedParent)
      throws Exception {
    Notebook acceptedNotebook = notebookRepository.findById(notebook.getId()).orElseThrow();
    ResponseEntity<byte[]> downloaded = controller.downloadNotebookGitBundle(acceptedNotebook);
    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit downloadedCommit =
          GitBundleTestReader.fetchSingleParentCommit(readBack, downloaded.getBody());
      assertThat(downloadedCommit.head(), equalTo(proposedCommit.head()));
      assertThat(downloadedCommit.tree(), equalTo(proposedCommit.tree()));
      assertThat(downloadedCommit.parent(), equalTo(expectedParent));
      if (expectedParentOfAcceptedParent != null) {
        try (RevWalk revWalk = new RevWalk(readBack)) {
          RevCommit saveCommit = revWalk.parseCommit(expectedParent);
          assertThat(saveCommit.getParentCount(), is(1));
          assertThat(saveCommit.getParent(0).getId(), equalTo(expectedParentOfAcceptedParent));
        }
      }
      assertThat(
          NotebookGitProposalBlobText.readUtf8(readBack, downloadedCommit.head(), ALPHA_PATH),
          equalTo(LOCAL_CONTENT));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(readBack, downloadedCommit.head(), BETA_PATH),
          equalTo(betaContent));
    }
  }

  private record NotebookWithCreatedBeta(
      Notebook notebook,
      Note alpha,
      Integer betaId,
      NotebookGitBinding accepted,
      ObjectId acceptedHead,
      String betaContent) {}

  private record AlphaBetaTrackers(MemoryTracker alphaTracker, MemoryTracker betaTracker) {}

  private record PublishedProposal(
      String publishedHead, GitBundleTestReader.SingleParentGitCommit proposedCommit) {}
}
