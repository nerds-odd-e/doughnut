package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.testability.GitBundleTestReader;
import java.sql.Timestamp;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Proves a mixed composed publish range (exact move, deletion, addition) rolls back entirely with
 * the existing publish transaction when late binding acceptance fails — accepted head A, live IDs,
 * dependent rows and bundle remain intact. Reuses {@link NotebookGitPublicationAtomicTestSupport}.
 */
@ActiveProfiles({"test", "notebook-git-publication-atomic-test"})
@Import(NotebookGitPublicationAtomicTestSupport.FailingBindingSaveConfig.class)
class NotebookGitComposedRangePublicationAtomicControllerTest
    extends NotebookGitControllerTestBase {

  private static final String MOVED_ORIGINAL = "---\ntype: Note\n---\nMoved authored bytes.\n";
  private static final String MOVED_EDITED = "---\ntype: Note\n---\nMoved edited bytes.\n";
  private static final String DELETED_ORIGINAL = "---\ntype: Note\n---\nDeleted authored bytes.\n";
  private static final String COMPANION_ORIGINAL = "---\ntype: Note\n---\nCompanion original.\n";
  private static final String COMPANION_EDITED = "---\ntype: Note\n---\nCompanion edited.\n";
  private static final String ADDED_CONTENT = "---\ntype: Note\n---\nNewly added authored bytes.\n";

  @Autowired MemoryTrackerRepository memoryTrackerRepository;

  @AfterEach
  void resetFailureInjection() {
    NotebookGitPublicationAtomicTestSupport.FAIL_ON_BINDING_SAVE.set(false);
  }

  @Test
  void lateBindingSaveFailureRollsBackMixedMoveDeleteAddRangeLeavingAcceptedStateA()
      throws Exception {
    Notebook notebook = createProductLfsNotebook();
    Note moved =
        makeMe.aNote().notebook(notebook).title("Original").content(MOVED_ORIGINAL).please();
    Note deleted =
        makeMe.aNote().notebook(notebook).title("Deleted").content(DELETED_ORIGINAL).please();
    Note companion =
        makeMe.aNote().notebook(notebook).title("Companion").content(COMPANION_ORIGINAL).please();
    MemoryTracker movedTracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(moved.getId()).orElseThrow())
                    .nextRecallAt(makeMe.aTimestamp().of(5, 0).please())
                    .please());
    MemoryTracker deletedTracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(deleted.getId()).orElseThrow())
                    .difficulty(7f)
                    .please());
    inCommittedTransaction(
        transactionManager,
        () -> {
          makeMe
              .aRecallPrompt()
              .forMemoryTracker(
                  memoryTrackerRepository.findById(deletedTracker.getId()).orElseThrow())
              .withMcqForNote(noteRepository.findById(deleted.getId()).orElseThrow())
              .please();
          makeMe.anImage().forNote(noteRepository.findById(deleted.getId()).orElseThrow()).please();
          makeMe
              .aConversation()
              .forANote(noteRepository.findById(deleted.getId()).orElseThrow())
              .please();
        });
    snapshotCurrentPortableTree(notebook);
    NotebookGitBinding binding =
        inCommittedTransaction(
            transactionManager,
            () -> notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow());
    var acceptedBefore = acceptedHistory(notebook);
    String acceptedHead = binding.getAcceptedGitObjectId();
    Timestamp bindingUpdatedAt = binding.getUpdatedAt();
    Timestamp movedUpdatedAt =
        inCommittedTransaction(
            transactionManager,
            () -> noteRepository.findById(moved.getId()).orElseThrow().getUpdatedAt());
    Timestamp companionUpdatedAt =
        inCommittedTransaction(
            transactionManager,
            () -> noteRepository.findById(companion.getId()).orElseThrow().getUpdatedAt());
    DependentCounts deletedDependentsBefore =
        inCommittedTransaction(transactionManager, () -> dependentCounts(deleted));
    Timestamp movedTrackerNextRecallAt = movedTracker.getNextRecallAt();

    ObjectId acceptedHeadId = ObjectId.fromString(acceptedHead);
    byte[] proposal;
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.fetchHead(repository, acceptedBundleBytes(notebook));
      ObjectId afterMoveDeleteAndCompanionEdit =
          commitOnTopOf(
              repository,
              List.of(acceptedHeadId),
              withAcceptedMetadata(
                  repository,
                  acceptedHeadId,
                  List.of(
                      new NotebookGitProposalFile("Renamed.md", MOVED_ORIGINAL),
                      new NotebookGitProposalFile("Companion.md", COMPANION_EDITED))),
              "Exact rename, delete learned note, edit companion");
      ObjectId tip =
          commitOnTopOf(
              repository,
              List.of(afterMoveDeleteAndCompanionEdit),
              withAcceptedMetadata(
                  repository,
                  acceptedHeadId,
                  List.of(
                      new NotebookGitProposalFile("Renamed.md", MOVED_EDITED),
                      new NotebookGitProposalFile("Companion.md", COMPANION_EDITED),
                      new NotebookGitProposalFile("Added.md", ADDED_CONTENT))),
              "Edit moved note and add unrelated note");
      proposal = bundleBytesForHead(repository, tip);
    }

    NotebookGitPublicationAtomicTestSupport.FAIL_ON_BINDING_SAVE.set(true);

    RuntimeException failure =
        assertThrows(
            RuntimeException.class,
            () -> controller.publishNotebookGitProposal(notebook.getId(), acceptedHead, proposal));
    assertThat(failure.getMessage(), is("forced failure after note projection"));

    inCommittedTransaction(
        transactionManager,
        () -> {
          NotebookGitBinding reloadedBinding =
              notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
          List<Note> storedNotes = noteRepository.findAllByNotebookIdOrderByIdAsc(notebook.getId());
          assertThat(storedNotes, hasSize(3));
          assertThat(
              storedNotes.stream().map(Note::getId).toList(),
              containsInAnyOrder(moved.getId(), deleted.getId(), companion.getId()));

          Note reloadedMoved = noteRepository.findById(moved.getId()).orElseThrow();
          assertThat(reloadedMoved.getTitle(), is("Original"));
          assertThat(reloadedMoved.getContent(), equalTo(MOVED_ORIGINAL));
          assertThat(reloadedMoved.getUpdatedAt(), is(movedUpdatedAt));

          Note reloadedDeleted = noteRepository.findById(deleted.getId()).orElseThrow();
          assertThat(reloadedDeleted.getTitle(), is("Deleted"));
          assertThat(reloadedDeleted.getContent(), equalTo(DELETED_ORIGINAL));
          assertThat(dependentCounts(deleted), equalTo(deletedDependentsBefore));
          MemoryTracker reloadedDeletedTracker =
              memoryTrackerRepository.findById(deletedTracker.getId()).orElseThrow();
          assertThat(reloadedDeletedTracker.isActive(), equalTo(true));
          assertThat(
              reloadedDeletedTracker.getDifficulty(), equalTo(deletedTracker.getDifficulty()));

          Note reloadedCompanion = noteRepository.findById(companion.getId()).orElseThrow();
          assertThat(reloadedCompanion.getContent(), equalTo(COMPANION_ORIGINAL));
          assertThat(reloadedCompanion.getUpdatedAt(), is(companionUpdatedAt));

          MemoryTracker reloadedMovedTracker =
              memoryTrackerRepository.findById(movedTracker.getId()).orElseThrow();
          assertThat(reloadedMovedTracker.getNote().getId(), equalTo(moved.getId()));
          assertThat(reloadedMovedTracker.getNextRecallAt(), is(movedTrackerNextRecallAt));

          assertThat(reloadedBinding.getAcceptedGitObjectId(), is(acceptedHead));
          assertThat(reloadedBinding.getUpdatedAt(), is(bindingUpdatedAt));
        });
    assertThat(acceptedHistory(notebook), equalTo(acceptedBefore));
  }
}
