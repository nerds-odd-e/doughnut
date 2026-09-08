package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.controllers.dto.NoteRecallInfo;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Verifies an edits-only revision of existing learned notes across Note projection and Git history.
 */
class NotebookGitExistingNoteBatchPublicationControllerTest
    extends NotebookGitWebContentControllerTestBase {

  private static final String ORIGINAL_CONTENT = "---\ntype: Note\n---\nOriginal authored bytes.\n";
  private static final String WEB_CONTENT = "---\ntype: Note\n---\nWeb save on the third note.\n";
  private static final String PUBLISHED_CONTENT =
      "---\n"
          + "type: FieldObservation\n"
          + "confidence: 7\n"
          + "reviewed: false\n"
          + "---\n"
          + "Precisely preserved authored bytes linking [[Reference Target|the target]].\n";

  @Autowired MemoryTrackerRepository memoryTrackerRepository;

  @Test
  void publishesEditsOnlyRevisionOnOriginalLearnedNotesAndMakesItDownloadable() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note root = makeMe.aNote().notebook(notebook).title("First").content(ORIGINAL_CONTENT).please();
    Folder folder = makeMe.aFolder().notebook(notebook).name("Research").please();
    Note nested =
        makeMe.aNote().folder(folder).title("Refined Note").content(ORIGINAL_CONTENT).please();
    MemoryTracker rootTracker = learnedTracker(root, 7f);
    MemoryTracker nestedTracker = learnedTracker(nested, 4f);
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    ObjectId acceptedHead = ObjectId.fromString(binding.getAcceptedGitObjectId());
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("First.md", PUBLISHED_CONTENT),
                new NotebookGitProposalFile("Research/Refined Note.md", PUBLISHED_CONTENT)));

    GitBundleTestReader.SingleParentGitCommit proposedCommit;
    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription())) {
      proposedCommit = GitBundleTestReader.fetchSingleParentCommit(proposal, proposalBytes);
      assertThat(proposedCommit.parent(), equalTo(acceptedHead));
    }

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    assertThat(publishedHead, equalTo(proposedCommit.head().getName()));
    assertShownContentAndRetainedLearning(root, rootTracker, PUBLISHED_CONTENT);
    assertShownContentAndRetainedLearning(nested, nestedTracker, PUBLISHED_CONTENT);

    Notebook acceptedNotebook = notebookRepository.findById(notebook.getId()).orElseThrow();
    ResponseEntity<byte[]> downloaded = controller.downloadNotebookGitBundle(acceptedNotebook);
    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit downloadedCommit =
          GitBundleTestReader.fetchSingleParentCommit(readBack, downloaded.getBody());
      assertThat(downloadedCommit.head(), equalTo(proposedCommit.head()));
      assertThat(downloadedCommit.tree(), equalTo(proposedCommit.tree()));
      assertThat(downloadedCommit.parent(), equalTo(acceptedHead));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(readBack, downloadedCommit.head(), "First.md"),
          equalTo(PUBLISHED_CONTENT));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(
              readBack, downloadedCommit.head(), "Research/Refined Note.md"),
          equalTo(PUBLISHED_CONTENT));
    }
  }

  @Test
  void keepsAllThreeLearnedNotesWhenPublishingTwoNoteRevisionOntoAcceptedWebSave()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note noteA =
        makeMe.aNote().notebook(notebook).title("First").content(ORIGINAL_CONTENT).please();
    Folder folder = makeMe.aFolder().notebook(notebook).name("Research").please();
    Note noteB =
        makeMe.aNote().folder(folder).title("Refined Note").content(ORIGINAL_CONTENT).please();
    Note noteC =
        makeMe.aNote().notebook(notebook).title("Other").content(ORIGINAL_CONTENT).please();
    MemoryTracker trackerA = learnedTracker(noteA, 7f);
    MemoryTracker trackerB = learnedTracker(noteB, 4f);
    MemoryTracker trackerC = learnedTracker(noteC, 5f);
    snapshotCurrentPortableTree(notebook);

    textContentController.updateNoteContent(noteC, contentDto(WEB_CONTENT));
    NotebookGitBinding afterWebSave = binding(notebook);
    ObjectId webSaveHead = ObjectId.fromString(afterWebSave.getAcceptedGitObjectId());
    byte[] proposalBytes =
        proposalBundleBytes(
            afterWebSave,
            List.of(
                new NotebookGitProposalFile("First.md", PUBLISHED_CONTENT),
                new NotebookGitProposalFile("Research/Refined Note.md", PUBLISHED_CONTENT),
                new NotebookGitProposalFile("Other.md", WEB_CONTENT)));

    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit proposedCommit =
          GitBundleTestReader.fetchSingleParentCommit(proposal, proposalBytes);
      assertThat(proposedCommit.parent(), equalTo(webSaveHead));
    }

    controller.publishNotebookGitProposal(
        notebook.getId(), afterWebSave.getAcceptedGitObjectId(), proposalBytes);

    assertShownContentAndRetainedLearning(noteA, trackerA, PUBLISHED_CONTENT);
    assertShownContentAndRetainedLearning(noteB, trackerB, PUBLISHED_CONTENT);
    assertShownContentAndRetainedLearning(noteC, trackerC, WEB_CONTENT);
  }

  @Test
  void refusesTheCompleteBatchWhenOneEditedNoteIsInvalid() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note root = makeMe.aNote().notebook(notebook).title("First").content(ORIGINAL_CONTENT).please();
    Folder folder = makeMe.aFolder().notebook(notebook).name("Research").please();
    Note nested =
        makeMe.aNote().folder(folder).title("Refined Note").content(ORIGINAL_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("First.md", PUBLISHED_CONTENT),
                new NotebookGitProposalFile(
                    "Research/Refined Note.md", "---\ncustom: value\n---\nChanged body.\n")));

    assertProposalRejectedWithoutMutatingBinding(
        notebook, binding.getAcceptedGitObjectId(), proposalBytes, HttpStatus.BAD_REQUEST);

    NoteRealm rootView = noteController.showNote(root);
    assertThat(rootView.getId(), equalTo(root.getId()));
    assertThat(rootView.getNote().getContent(), equalTo(ORIGINAL_CONTENT));
    NoteRealm nestedView = noteController.showNote(nested);
    assertThat(nestedView.getId(), equalTo(nested.getId()));
    assertThat(nestedView.getNote().getContent(), equalTo(ORIGINAL_CONTENT));
  }

  private MemoryTracker learnedTracker(Note note, float difficulty) {
    return inCommittedTransaction(
        transactionManager,
        () ->
            makeMe
                .aMemoryTrackerFor(noteRepository.findById(note.getId()).orElseThrow())
                .difficulty(difficulty)
                .please());
  }

  private void assertShownContentAndRetainedLearning(
      Note original, MemoryTracker tracker, String expectedContent)
      throws UnexpectedNoAccessRightException {
    Note reloaded = noteRepository.findById(original.getId()).orElseThrow();
    NoteRealm view = noteController.showNote(reloaded);
    assertThat(view.getId(), equalTo(original.getId()));
    assertThat(view.getNote().getContent(), equalTo(expectedContent));
    NoteRecallInfo recallInfo = noteController.getNoteInfo(reloaded);
    assertThat(recallInfo.getMemoryTrackers(), hasSize(1));
    assertThat(recallInfo.getMemoryTrackers().getFirst().getId(), equalTo(tracker.getId()));
    MemoryTracker retained = memoryTrackerRepository.findById(tracker.getId()).orElseThrow();
    assertThat(retained.getDifficulty(), equalTo(tracker.getDifficulty()));
    assertThat(retained.getStability(), equalTo(tracker.getStability()));
    assertThat(retained.getLastRecalledAt(), equalTo(tracker.getLastRecalledAt()));
    assertThat(retained.getNextRecallAt(), equalTo(tracker.getNextRecallAt()));
    assertThat(retained.getAssimilatedAt(), equalTo(tracker.getAssimilatedAt()));
    assertThat(retained.getRemovedFromTracking(), equalTo(tracker.getRemovedFromTracking()));
    assertThat(retained.getType(), equalTo(tracker.getType()));
    assertThat(retained.getPropertyKey(), equalTo(tracker.getPropertyKey()));
  }
}
