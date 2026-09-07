package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;

import com.odde.donut.entities.Conversation;
import com.odde.donut.entities.Mcq;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.repositories.ConversationRepository;
import com.odde.donut.entities.repositories.McqRepository;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;

/** Shared learned/untouched private-association fixture for Git publication identity proofs. */
abstract class NotebookGitMovedNotePrivateAssociationControllerTestBase
    extends NotebookGitBundleControllerTestBase {

  @Autowired ConversationRepository conversationRepository;
  @Autowired McqRepository mcqRepository;
  @Autowired MemoryTrackerRepository memoryTrackerRepository;

  private Integer conversationId;

  @AfterEach
  void removeConversationBeforeCommittedUserCleanup() {
    if (conversationId != null) {
      inCommittedTransaction(
          transactionManager, () -> conversationRepository.deleteById(conversationId));
    }
  }

  RelocatedNoteAssociations commitPrivateAssociations(Note relocated, Note untouched) {
    return inCommittedTransaction(
        transactionManager,
        () -> {
          Note reloaded = noteRepository.findById(relocated.getId()).orElseThrow();
          MemoryTracker relocatedTracker =
              makeMe
                  .aMemoryTrackerFor(reloaded)
                  .removedFromTracking()
                  .nextRecallAt(makeMe.aTimestamp().of(5, 0).please())
                  .please();
          MemoryTracker untouchedTracker =
              makeMe
                  .aMemoryTrackerFor(noteRepository.findById(untouched.getId()).orElseThrow())
                  .please();
          Mcq mcq = makeMe.anMcq().forNote(reloaded).please();
          Conversation conversation =
              makeMe.aConversation().forANote(reloaded).from(currentUser.getUser()).please();
          conversationId = conversation.getId();
          return new RelocatedNoteAssociations(
              relocatedTracker, untouchedTracker, mcq.getId(), conversation.getId());
        });
  }

  record RelocatedNoteAssociations(
      MemoryTracker relocatedTracker,
      MemoryTracker untouchedTracker,
      Integer mcqId,
      Integer conversationId) {}
}
