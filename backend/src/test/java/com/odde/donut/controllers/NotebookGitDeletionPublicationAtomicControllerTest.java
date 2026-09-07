package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"test", "notebook-git-publication-atomic-test"})
@Import(NotebookGitPublicationAtomicTestSupport.FailingBindingSaveConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NotebookGitDeletionPublicationAtomicControllerTest
    extends NotebookGitBundleControllerTestBase {

  private static final String ORIGINAL_CONTENT = "---\ntype: Note\n---\nOriginal authored bytes.\n";

  @Autowired MemoryTrackerRepository memoryTrackerRepository;

  @AfterEach
  void resetFailureInjection() {
    NotebookGitPublicationAtomicTestSupport.FAIL_ON_BINDING_SAVE.set(false);
  }

  @Test
  void lateBindingSaveFailureRollsBackIsolatedLearnedNoteDeletionAndAcceptedBinding()
      throws Exception {
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
                    .difficulty(7f)
                    .please());
    snapshotCurrentPortableTree(notebook);
    NotebookGitBinding binding =
        inCommittedTransaction(
            transactionManager,
            () -> notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow());
    byte[] acceptedBundle = binding.getBundleBytes();
    String acceptedHead = binding.getAcceptedGitObjectId();
    Timestamp bindingUpdatedAt = binding.getUpdatedAt();
    Timestamp noteUpdatedAt =
        inCommittedTransaction(
            transactionManager,
            () -> noteRepository.findById(target.getId()).orElseThrow().getUpdatedAt());
    byte[] proposal =
        proposalBundleBytes(
            binding, List.of(new NotebookGitProposalFile("Retained.md", ORIGINAL_CONTENT)));

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
          List<Note> liveNotes =
              noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId());
          assertThat(liveNotes, hasSize(2));
          assertThat(liveNotes.stream().map(Note::getId).toList(), hasItem(target.getId()));
          Note reloadedTarget = noteRepository.findById(target.getId()).orElseThrow();
          assertThat(reloadedTarget.getDeletedAt(), nullValue());
          assertThat(reloadedTarget.getUpdatedAt(), is(noteUpdatedAt));
          MemoryTracker reloadedTracker =
              memoryTrackerRepository.findById(targetTracker.getId()).orElseThrow();
          assertThat(reloadedTracker.getDeletedAt(), nullValue());
          assertThat(reloadedTracker.getDifficulty(), equalTo(targetTracker.getDifficulty()));
          assertThat(reloadedTracker.getStability(), equalTo(targetTracker.getStability()));
          assertThat(
              reloadedTracker.getLastRecalledAt(), equalTo(targetTracker.getLastRecalledAt()));
          assertThat(reloadedTracker.getNextRecallAt(), equalTo(targetTracker.getNextRecallAt()));
          assertThat(reloadedTracker.getAssimilatedAt(), equalTo(targetTracker.getAssimilatedAt()));
          assertThat(
              reloadedTracker.getRemovedFromTracking(),
              equalTo(targetTracker.getRemovedFromTracking()));
          assertThat(reloadedBinding.getAcceptedGitObjectId(), is(acceptedHead));
          assertThat(reloadedBinding.getBundleBytes(), equalTo(acceptedBundle));
          assertThat(reloadedBinding.getUpdatedAt(), is(bindingUpdatedAt));
        });
  }
}
