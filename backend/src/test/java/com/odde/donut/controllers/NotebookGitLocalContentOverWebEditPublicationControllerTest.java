package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.donut.controllers.dto.NoteRecallInfo;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Verifies that a one-child proposal of chosen local content, parented on an accepted web edit, is
 * accepted on the original learned identities — both when the web edit is a different note and when
 * it is the same learned note.
 */
class NotebookGitLocalContentOverWebEditPublicationControllerTest
    extends NotebookGitWebContentControllerTestBase {

  private static final String LOCAL_NOTE_PATH = "Local Note.md";
  private static final String WEB_NOTE_PATH = "Web Note.md";
  private static final String LEARNED_NOTE_PATH = "Learned Note.md";
  private static final String ORIGINAL_CONTENT = "---\ntype: Note\n---\noriginal content";
  private static final String LOCAL_CONTENT = "---\ntype: Note\n---\nlocal patch";
  private static final String WEB_CONTENT = "---\ntype: Note\n---\nweb edit";
  private static final String CHOSEN_CONTENT =
      "---\ntype: Note\nauthored: chosen\n---\nchosen resolution";

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
