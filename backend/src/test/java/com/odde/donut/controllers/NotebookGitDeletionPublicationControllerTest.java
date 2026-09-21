package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.controllers.dto.NoteRecallInfo;
import com.odde.donut.controllers.dto.WikiLink;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

/**
 * Verifies learned-note deletion publication — alone or with same-path edits — across Note
 * projection and Git history. An accepted file deletion permanently removes the note and its
 * complete dependent data; the container and other notes remain.
 */
class NotebookGitDeletionPublicationControllerTest extends NotebookGitControllerTestBase {

  private static final String ORIGINAL_CONTENT = "---\ntype: Note\n---\nOriginal authored bytes.\n";
  private static final String EDITED_CONTENT = "---\ntype: Note\n---\nEdited authored bytes.\n";
  private static final String REFERRER_CONTENT =
      "---\n" + "type: Note\n" + "example of: \"[[Target]]\"\n" + "---\n" + "Body [[Target]]\n";

  @Autowired NoteController noteController;
  @Autowired MemoryTrackerRepository memoryTrackerRepository;

  @Test
  void publishesLearnedNoteDeletionsWithASamePathEditAsTheExactAuthoredCommit() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note deletedA =
        makeMe.aNote().notebook(notebook).title("DeletedA").content(ORIGINAL_CONTENT).please();
    Note deletedB =
        makeMe.aNote().notebook(notebook).title("DeletedB").content(ORIGINAL_CONTENT).please();
    Note retained =
        makeMe.aNote().notebook(notebook).title("Retained").content(ORIGINAL_CONTENT).please();
    // Learned shape for deletedA: memory_tracker + recall_prompt + mcq, plus image + conversation.
    MemoryTracker deletedATracker = learnedTracker(deletedA, 7f);
    inCommittedTransaction(
        transactionManager,
        () -> {
          makeMe
              .aRecallPrompt()
              .forMemoryTracker(
                  memoryTrackerRepository.findById(deletedATracker.getId()).orElseThrow())
              .withMcqForNote(noteRepository.findById(deletedA.getId()).orElseThrow())
              .please();
          makeMe
              .anImage()
              .forNote(noteRepository.findById(deletedA.getId()).orElseThrow())
              .please();
          makeMe
              .aConversation()
              .forANote(noteRepository.findById(deletedA.getId()).orElseThrow())
              .please();
        });
    // Unlearned shape for deletedB: image + conversation, no memory_tracker.
    inCommittedTransaction(
        transactionManager,
        () -> {
          makeMe
              .anImage()
              .forNote(noteRepository.findById(deletedB.getId()).orElseThrow())
              .please();
          makeMe
              .aConversation()
              .forANote(noteRepository.findById(deletedB.getId()).orElseThrow())
              .please();
        });
    MemoryTracker retainedTracker = learnedTracker(retained, 5f);
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    ObjectId acceptedHead = ObjectId.fromString(binding.getAcceptedGitObjectId());
    byte[] proposalBytes =
        proposalBundleBytes(
            binding, List.of(new NotebookGitProposalFile("Retained.md", EDITED_CONTENT)));

    GitBundleTestReader.SingleParentGitCommit proposedCommit;
    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription())) {
      proposedCommit = GitBundleTestReader.fetchSingleParentCommit(proposal, proposalBytes);
      assertThat(proposedCommit.parent(), equalTo(acceptedHead));
    }

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    assertThat(publishedHead, equalTo(proposedCommit.head().getName()));
    List<Note> storedNotes = noteRepository.findAllByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(storedNotes, hasSize(1));
    assertThat(storedNotes.getFirst().getId(), equalTo(retained.getId()));
    // Both learned and unlearned deleted notes are permanently gone with their dependents.
    assertPermanentlyRemovedWithDependents(deletedA, deletedATracker);
    assertPermanentlyRemovedWithDependents(deletedB, null);
    Note reloadedRetained = noteRepository.findById(retained.getId()).orElseThrow();
    assertThat(reloadedRetained.getContent(), equalTo(EDITED_CONTENT));
    assertThat(
        inCommittedTransaction(
            transactionManager,
            () ->
                memoryTrackerRepository.findById(retainedTracker.getId()).orElseThrow().isActive()),
        equalTo(true));
    NoteRecallInfo retainedRecall = noteController.getNoteInfo(reloadedRetained);
    assertThat(retainedRecall.getMemoryTrackers(), hasSize(1));
    assertThat(
        retainedRecall.getMemoryTrackers().getFirst().getId(), equalTo(retainedTracker.getId()));

    Notebook acceptedNotebook = notebookRepository.findById(notebook.getId()).orElseThrow();
    ResponseEntity<byte[]> downloaded = controller.downloadNotebookGitBundle(acceptedNotebook);
    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit downloadedCommit =
          GitBundleTestReader.fetchSingleParentCommit(readBack, downloaded.getBody());
      assertThat(downloadedCommit.head(), equalTo(proposedCommit.head()));
      // The accepted tree no longer contains the deleted files.
      assertThat(downloadedCommit.tree(), equalTo(proposedCommit.tree()));
      assertThat(downloadedCommit.parent(), equalTo(acceptedHead));
    }
  }

  @Test
  void publishesADeletionOnlyBatchThatRemovesEveryOrdinaryNote() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe.aNote().notebook(notebook).title("First").content(ORIGINAL_CONTENT).please();
    makeMe.aNote().notebook(notebook).title("Second").content(ORIGINAL_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    ObjectId acceptedHead = ObjectId.fromString(binding.getAcceptedGitObjectId());
    byte[] proposalBytes = proposalBundleBytes(binding, List.of());

    GitBundleTestReader.SingleParentGitCommit proposedCommit;
    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription())) {
      proposedCommit = GitBundleTestReader.fetchSingleParentCommit(proposal, proposalBytes);
      assertThat(proposedCommit.parent(), equalTo(acceptedHead));
    }

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    assertThat(publishedHead, equalTo(proposedCommit.head().getName()));
    assertThat(noteRepository.findAllByNotebookIdOrderByIdAsc(notebook.getId()), empty());
    ResponseEntity<byte[]> downloaded =
        controller.downloadNotebookGitBundle(
            notebookRepository.findById(notebook.getId()).orElseThrow());
    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit downloadedCommit =
          GitBundleTestReader.fetchSingleParentCommit(readBack, downloaded.getBody());
      assertThat(downloadedCommit.head(), equalTo(proposedCommit.head()));
      assertThat(downloadedCommit.tree(), equalTo(proposedCommit.tree()));
      assertThat(downloadedCommit.parent(), equalTo(acceptedHead));
    }
  }

  @Test
  void leavesReferringBodyAndPropertyLinksAuthoredWhenPublishingTheTargetsDeletion()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note target =
        makeMe.aNote().notebook(notebook).title("Target").content(ORIGINAL_CONTENT).please();
    Note referrer =
        makeMe.aNote().notebook(notebook).title("Referrer").content(REFERRER_CONTENT).please();
    inCommittedTransaction(
        transactionManager,
        () ->
            makeMe.authorReferencingContent(
                noteRepository.findById(referrer.getId()).orElseThrow(), REFERRER_CONTENT));
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    List<WikiLink.Resolution> resolutionsBeforePublish =
        targetResolutions(
            noteController.showNote(noteRepository.findById(referrer.getId()).orElseThrow()));
    assertThat(resolutionsBeforePublish, not(empty()));
    assertThat(resolutionsBeforePublish, everyItem(equalTo(WikiLink.Resolution.RESOLVED)));
    byte[] proposalBytes =
        proposalBundleBytes(
            binding, List.of(new NotebookGitProposalFile("Referrer.md", REFERRER_CONTENT)));

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    // The target note is permanently removed; the referrer's authored wiki-link text remains.
    assertThat(noteRepository.findById(target.getId()).isPresent(), equalTo(false));
    NoteRealm shown =
        noteController.showNote(noteRepository.findById(referrer.getId()).orElseThrow());
    assertThat(shown.getNote().getContent(), equalTo(REFERRER_CONTENT));
    assertThat(targetResolutions(shown), empty());
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

  private void assertPermanentlyRemovedWithDependents(Note note, MemoryTracker tracker) {
    inCommittedTransaction(
        transactionManager,
        () -> {
          assertThat(noteRepository.findById(note.getId()).isPresent(), equalTo(false));
          assertThat(countRowsByNoteId("memory_tracker", note.getId()), equalTo(0L));
          assertThat(countRowsByNoteId("mcq", note.getId()), equalTo(0L));
          assertThat(countRowsByNoteId("image", note.getId()), equalTo(0L));
          assertThat(countRowsByNoteId("conversation", note.getId()), equalTo(0L));
          assertThat(countRecallPromptsByNoteId(note.getId()), equalTo(0L));
          if (tracker != null) {
            assertThat(
                memoryTrackerRepository.findById(tracker.getId()).isPresent(), equalTo(false));
          }
        });
  }

  private static List<WikiLink.Resolution> targetResolutions(NoteRealm shown) {
    return shown.getWikiLinks().stream()
        .filter(link -> "Target".equals(link.getTarget()))
        .map(WikiLink::getResolution)
        .toList();
  }
}
