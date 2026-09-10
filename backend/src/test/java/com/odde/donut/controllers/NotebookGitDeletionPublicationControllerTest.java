package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.controllers.dto.NoteRecallInfo;
import com.odde.donut.controllers.dto.WikiLink;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.testability.GitBundleTestReader;
import java.sql.Timestamp;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

/**
 * Verifies learned-note deletion publication — alone or with same-path edits — across Note
 * projection and Git history.
 */
class NotebookGitDeletionPublicationControllerTest extends NotebookGitBundleControllerTestBase {

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
    MemoryTracker deletedATracker = learnedTracker(deletedA, 7f);
    MemoryTracker deletedBTracker = learnedTracker(deletedB, 6f);
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
    List<Note> liveNotes = noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(liveNotes, hasSize(1));
    assertThat(liveNotes.getFirst().getId(), equalTo(retained.getId()));
    assertSoftDeletedWithLearning(deletedA, deletedATracker);
    assertSoftDeletedWithLearning(deletedB, deletedBTracker);
    Note reloadedRetained = noteRepository.findById(retained.getId()).orElseThrow();
    assertThat(reloadedRetained.getContent(), equalTo(EDITED_CONTENT));
    MemoryTracker stillLiveTracker =
        memoryTrackerRepository.findById(retainedTracker.getId()).orElseThrow();
    assertThat(stillLiveTracker.getDeletedAt(), nullValue());
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
    assertThat(noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()), empty());
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
  void retriesAnAcceptedDeletionWithoutChangingHeadOrDeletionTimestamps() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note target =
        makeMe.aNote().notebook(notebook).title("Target").content(ORIGINAL_CONTENT).please();
    makeMe.aNote().notebook(notebook).title("Retained").content(ORIGINAL_CONTENT).please();
    MemoryTracker targetTracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(target.getId()).orElseThrow())
                    .please());
    NotebookGitBinding initialBinding = snapshotCurrentPortableTree(notebook);
    String initialHead = initialBinding.getAcceptedGitObjectId();
    byte[] proposalBytes =
        proposalBundleBytes(
            initialBinding, List.of(new NotebookGitProposalFile("Retained.md", ORIGINAL_CONTENT)));

    String publishedHead =
        controller.publishNotebookGitProposal(notebook.getId(), initialHead, proposalBytes);
    PublicationState stateAfterPublication = publicationState(notebook, target, targetTracker);

    testabilitySettings.timeTravelTo(Timestamp.valueOf("2020-06-01 00:00:00"));
    String retriedHead =
        controller.publishNotebookGitProposal(notebook.getId(), initialHead, proposalBytes);

    assertThat(retriedHead, equalTo(publishedHead));
    PublicationState stateAfterRetry = publicationState(notebook, target, targetTracker);
    assertThat(stateAfterRetry.acceptedHead(), equalTo(stateAfterPublication.acceptedHead()));
    assertThat(
        stateAfterRetry.bindingUpdatedAt(), equalTo(stateAfterPublication.bindingUpdatedAt()));
    assertThat(stateAfterRetry.bundleBytes(), equalTo(stateAfterPublication.bundleBytes()));
    assertThat(stateAfterRetry.noteDeletedAt(), equalTo(stateAfterPublication.noteDeletedAt()));
    assertThat(
        stateAfterRetry.trackerDeletedAt(), equalTo(stateAfterPublication.trackerDeletedAt()));
  }

  @Test
  void leavesReferringBodyAndPropertyLinksAuthoredWhenPublishingTheTargetsDeletion()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
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

  private void assertSoftDeletedWithLearning(Note note, MemoryTracker tracker)
      throws UnexpectedNoAccessRightException {
    Note reloaded = noteRepository.findById(note.getId()).orElseThrow();
    assertThat(reloaded.getDeletedAt(), notNullValue());
    MemoryTracker deletedTracker = memoryTrackerRepository.findById(tracker.getId()).orElseThrow();
    assertThat(deletedTracker.getDeletedAt(), notNullValue());
    assertThat(deletedTracker.getDifficulty(), equalTo(tracker.getDifficulty()));
    assertThat(deletedTracker.getStability(), equalTo(tracker.getStability()));
    assertThat(noteController.getNoteInfo(reloaded).getMemoryTrackers(), hasSize(0));
  }

  private static List<WikiLink.Resolution> targetResolutions(NoteRealm shown) {
    return shown.getWikiLinks().stream()
        .filter(link -> "Target".equals(link.getTarget()))
        .map(WikiLink::getResolution)
        .toList();
  }

  private PublicationState publicationState(Notebook notebook, Note note, MemoryTracker tracker) {
    return inCommittedTransaction(
        transactionManager,
        () -> {
          NotebookGitBinding binding =
              notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
          Note reloadedNote = noteRepository.findById(note.getId()).orElseThrow();
          MemoryTracker reloadedTracker =
              memoryTrackerRepository.findById(tracker.getId()).orElseThrow();
          return new PublicationState(
              binding.getAcceptedGitObjectId(),
              binding.getUpdatedAt(),
              binding.getBundleBytes().clone(),
              reloadedNote.getDeletedAt(),
              reloadedTracker.getDeletedAt());
        });
  }

  private record PublicationState(
      String acceptedHead,
      Timestamp bindingUpdatedAt,
      byte[] bundleBytes,
      Timestamp noteDeletedAt,
      Timestamp trackerDeletedAt) {}
}
