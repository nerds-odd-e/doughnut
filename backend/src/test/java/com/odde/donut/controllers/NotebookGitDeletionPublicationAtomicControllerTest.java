package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
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
  private static final String EDITED_CONTENT = "---\ntype: Note\n---\nEdited authored bytes.\n";

  @Autowired MemoryTrackerRepository memoryTrackerRepository;

  @AfterEach
  void resetFailureInjection() {
    NotebookGitPublicationAtomicTestSupport.FAIL_ON_BINDING_SAVE.set(false);
  }

  @Test
  void lateBindingSaveFailureRollsBackMixedDeletionEditRevisionAndAcceptedBinding()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note deletedA =
        makeMe.aNote().notebook(notebook).title("DeletedA").content(ORIGINAL_CONTENT).please();
    Note deletedB =
        makeMe.aNote().notebook(notebook).title("DeletedB").content(ORIGINAL_CONTENT).please();
    Note retained =
        makeMe.aNote().notebook(notebook).title("Retained").content(ORIGINAL_CONTENT).please();
    // Learned shape for deletedA: memory_tracker + recall_prompt + mcq + image + conversation.
    MemoryTracker deletedATracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(deletedA.getId()).orElseThrow())
                    .difficulty(7f)
                    .please());
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
    snapshotCurrentPortableTree(notebook);
    NotebookGitBinding binding =
        inCommittedTransaction(
            transactionManager,
            () -> notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow());
    byte[] acceptedBundle = binding.getBundleBytes();
    String acceptedHead = binding.getAcceptedGitObjectId();
    Timestamp bindingUpdatedAt = binding.getUpdatedAt();
    Timestamp retainedUpdatedAt =
        inCommittedTransaction(
            transactionManager,
            () -> noteRepository.findById(retained.getId()).orElseThrow().getUpdatedAt());
    DependentCounts deletedADependentsBefore =
        inCommittedTransaction(transactionManager, () -> dependentCounts(deletedA));
    DependentCounts deletedBDependentsBefore =
        inCommittedTransaction(transactionManager, () -> dependentCounts(deletedB));
    byte[] proposal =
        proposalBundleBytes(
            binding, List.of(new NotebookGitProposalFile("Retained.md", EDITED_CONTENT)));

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
          assertThat(liveNotes, hasSize(3));
          assertThat(
              liveNotes.stream().map(Note::getId).toList(),
              hasItems(deletedA.getId(), deletedB.getId(), retained.getId()));
          // The would-be-deleted notes were hard-deleted inside the publication transaction; the
          // binding-save failure rolled that back, so the rows survive and were never soft-deleted.
          Note reloadedDeletedA = noteRepository.findById(deletedA.getId()).orElseThrow();
          assertThat(reloadedDeletedA.getContent(), equalTo(ORIGINAL_CONTENT));
          assertThat(reloadedDeletedA.getDeletedAt(), nullValue());
          Note reloadedDeletedB = noteRepository.findById(deletedB.getId()).orElseThrow();
          assertThat(reloadedDeletedB.getContent(), equalTo(ORIGINAL_CONTENT));
          assertThat(reloadedDeletedB.getDeletedAt(), nullValue());
          Note reloadedRetained = noteRepository.findById(retained.getId()).orElseThrow();
          assertThat(reloadedRetained.getContent(), equalTo(ORIGINAL_CONTENT));
          assertThat(reloadedRetained.getUpdatedAt(), is(retainedUpdatedAt));
          MemoryTracker reloadedTracker =
              memoryTrackerRepository.findById(deletedATracker.getId()).orElseThrow();
          assertThat(reloadedTracker.isActive(), equalTo(true));
          assertThat(reloadedTracker.getDifficulty(), equalTo(deletedATracker.getDifficulty()));
          assertThat(reloadedTracker.getStability(), equalTo(deletedATracker.getStability()));
          assertThat(
              reloadedTracker.getLastRecalledAt(), equalTo(deletedATracker.getLastRecalledAt()));
          assertThat(reloadedTracker.getNextRecallAt(), equalTo(deletedATracker.getNextRecallAt()));
          assertThat(
              reloadedTracker.getAssimilatedAt(), equalTo(deletedATracker.getAssimilatedAt()));
          assertThat(
              reloadedTracker.getRemovedFromTracking(),
              equalTo(deletedATracker.getRemovedFromTracking()));
          // The complete dependent closure (recall_prompt, mcq, image, conversation + messages)
          // was CASCADE-deleted inside the publication transaction and restored by its rollback.
          assertThat(dependentCounts(deletedA), equalTo(deletedADependentsBefore));
          assertThat(dependentCounts(deletedB), equalTo(deletedBDependentsBefore));
          assertThat(reloadedBinding.getAcceptedGitObjectId(), is(acceptedHead));
          assertThat(reloadedBinding.getBundleBytes(), equalTo(acceptedBundle));
          assertThat(reloadedBinding.getUpdatedAt(), is(bindingUpdatedAt));
        });
  }

  private DependentCounts dependentCounts(Note note) {
    return new DependentCounts(
        countRowsByNoteId("memory_tracker", note.getId()),
        countRecallPromptsByNoteId(note.getId()),
        countRowsByNoteId("mcq", note.getId()),
        countRowsByNoteId("image", note.getId()),
        countRowsByNoteId("conversation", note.getId()),
        countConversationMessagesByNoteId(note.getId()));
  }

  private long countConversationMessagesByNoteId(Integer noteId) {
    return ((Number)
            entityManager
                .createNativeQuery(
                    "SELECT COUNT(*) FROM conversation_message cm "
                        + "JOIN conversation c ON cm.conversation_id = c.id "
                        + "WHERE c.note_id = :id")
                .setParameter("id", noteId)
                .getSingleResult())
        .longValue();
  }

  private record DependentCounts(
      long memoryTracker,
      long recallPrompt,
      long mcq,
      long image,
      long conversation,
      long conversationMessage) {}
}
