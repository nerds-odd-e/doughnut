package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Folder;
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

/**
 * Proves {@code NotebookGitProposalPublisher#applyRename} rolls back folder and title with the rest
 * of the publish transaction when late binding acceptance fails, reusing {@link
 * NotebookGitPublicationAtomicTestSupport}'s exact failure injection and reset hook.
 */
@ActiveProfiles({"test", "notebook-git-publication-atomic-test"})
@Import(NotebookGitPublicationAtomicTestSupport.FailingBindingSaveConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NotebookGitProposalRenameRollbackControllerTest extends NotebookGitBundleControllerTestBase {

  private static final String TYPED_NOTE_CONTENT = "---\ntype: Note\n---\noriginal content";

  @Autowired MemoryTrackerRepository memoryTrackerRepository;

  @AfterEach
  void resetFailureInjection() {
    NotebookGitPublicationAtomicTestSupport.FAIL_ON_BINDING_SAVE.set(false);
  }

  @Test
  void lateBindingSaveFailureRollsBackARenameLeavingTheOldTitleTrackerAndAcceptedBinding()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note =
        makeMe.aNote().notebook(notebook).title("note").content(TYPED_NOTE_CONTENT).please();
    MemoryTracker tracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(note.getId()).orElseThrow())
                    .removedFromTracking()
                    .nextRecallAt(makeMe.aTimestamp().of(5, 0).please())
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
            () -> noteRepository.findById(note.getId()).orElseThrow().getUpdatedAt());
    Boolean trackerRemovedFromTracking = tracker.getRemovedFromTracking();
    Timestamp trackerNextRecallAt = tracker.getNextRecallAt();
    byte[] proposal =
        proposalBundleBytes(
            binding, List.of(new NotebookGitProposalFile("renamed.md", TYPED_NOTE_CONTENT)));

    NotebookGitPublicationAtomicTestSupport.FAIL_ON_BINDING_SAVE.set(true);

    RuntimeException failure =
        assertThrows(
            RuntimeException.class,
            () -> controller.publishNotebookGitProposal(notebook.getId(), acceptedHead, proposal));
    assertThat(failure.getMessage(), is("forced failure after note projection"));

    inCommittedTransaction(
        transactionManager,
        () -> {
          Note reloadedNote = noteRepository.findById(note.getId()).orElseThrow();
          assertThat(reloadedNote.getTitle(), is("note"));
          assertThat(reloadedNote.getUpdatedAt(), is(noteUpdatedAt));
          MemoryTracker reloadedTracker =
              memoryTrackerRepository.findById(tracker.getId()).orElseThrow();
          assertThat(reloadedTracker.getRemovedFromTracking(), is(trackerRemovedFromTracking));
          assertThat(reloadedTracker.getNextRecallAt(), is(trackerNextRecallAt));
          NotebookGitBinding reloadedBinding =
              notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
          assertThat(reloadedBinding.getAcceptedGitObjectId(), is(acceptedHead));
          assertThat(reloadedBinding.getBundleBytes(), equalTo(acceptedBundle));
          assertThat(reloadedBinding.getUpdatedAt(), is(bindingUpdatedAt));
        });
  }

  @Test
  void
      lateBindingSaveFailureRollsBackARelocateAndRenameLeavingTheOldFolderTitleTrackerAndAcceptedBinding()
          throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder source = makeMe.aFolder().notebook(notebook).name("Source").please();
    Folder destination = makeMe.aFolder().notebook(notebook).name("Dest").please();
    makeMe.aNote().folder(destination).title("other").content(TYPED_NOTE_CONTENT).please();
    Note note = makeMe.aNote().folder(source).title("note").content(TYPED_NOTE_CONTENT).please();
    MemoryTracker tracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(note.getId()).orElseThrow())
                    .removedFromTracking()
                    .nextRecallAt(makeMe.aTimestamp().of(5, 0).please())
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
            () -> noteRepository.findById(note.getId()).orElseThrow().getUpdatedAt());
    Boolean trackerRemovedFromTracking = tracker.getRemovedFromTracking();
    Timestamp trackerNextRecallAt = tracker.getNextRecallAt();
    byte[] proposal =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Dest/other.md", TYPED_NOTE_CONTENT),
                new NotebookGitProposalFile("Dest/renamed.md", TYPED_NOTE_CONTENT)));

    NotebookGitPublicationAtomicTestSupport.FAIL_ON_BINDING_SAVE.set(true);

    RuntimeException failure =
        assertThrows(
            RuntimeException.class,
            () -> controller.publishNotebookGitProposal(notebook.getId(), acceptedHead, proposal));
    assertThat(failure.getMessage(), is("forced failure after note projection"));

    inCommittedTransaction(
        transactionManager,
        () -> {
          Note reloadedNote = noteRepository.findById(note.getId()).orElseThrow();
          assertThat(reloadedNote.getFolder().getId(), is(source.getId()));
          assertThat(reloadedNote.getTitle(), is("note"));
          assertThat(reloadedNote.getUpdatedAt(), is(noteUpdatedAt));
          MemoryTracker reloadedTracker =
              memoryTrackerRepository.findById(tracker.getId()).orElseThrow();
          assertThat(reloadedTracker.getRemovedFromTracking(), is(trackerRemovedFromTracking));
          assertThat(reloadedTracker.getNextRecallAt(), is(trackerNextRecallAt));
          NotebookGitBinding reloadedBinding =
              notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
          assertThat(reloadedBinding.getAcceptedGitObjectId(), is(acceptedHead));
          assertThat(reloadedBinding.getBundleBytes(), equalTo(acceptedBundle));
          assertThat(reloadedBinding.getUpdatedAt(), is(bindingUpdatedAt));
        });
  }
}
