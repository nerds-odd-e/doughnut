package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.entities.Conversation;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Mcq;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.ConversationRepository;
import com.odde.donut.entities.repositories.McqRepository;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Verifies a learned note relocated across parents keeps its private associations, while an
 * untouched identical-content note keeps its own. The canonical private-association shape for
 * same-parent rename is covered in {@link NotebookGitProposalRenameControllerTest}.
 * Filename-preserving placement is covered in {@link NotebookGitProposalRelocationControllerTest}.
 */
class NotebookGitProposalRelocationPrivateAssociationControllerTest
    extends NotebookGitBundleControllerTestBase {

  private static final String TYPED_NOTE_CONTENT = "---\ntype: Note\n---\noriginal content";
  private static final String MATCHING_CONTENT = "---\ntype: Note\n---\nmatching learned content";

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

  @Test
  void
      preservesPrivateAssociationsWhenRelocatingAcrossParentsAndLeavesTheIdenticalTextNoteUntouched()
          throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder destination = makeMe.aFolder().notebook(notebook).name("Dest").please();
    makeMe.aNote().folder(destination).title("keep").content(TYPED_NOTE_CONTENT).please();
    Note relocatedNote =
        makeMe.aNote().notebook(notebook).title("Original").content(MATCHING_CONTENT).please();
    Note untouchedNote =
        makeMe.aNote().notebook(notebook).title("Untouched").content(MATCHING_CONTENT).please();
    RelocatedNoteAssociations associations =
        commitPrivateAssociations(relocatedNote, untouchedNote);
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);

    controller.publishNotebookGitProposal(
        notebook.getId(),
        binding.getAcceptedGitObjectId(),
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Dest/keep.md", TYPED_NOTE_CONTENT),
                new NotebookGitProposalFile("Dest/Original.md", MATCHING_CONTENT),
                new NotebookGitProposalFile("Untouched.md", MATCHING_CONTENT))));

    MemoryTracker relocatedTracker =
        memoryTrackerRepository.findById(associations.relocatedTracker().getId()).orElseThrow();
    assertThat(relocatedTracker.getNote().getId(), equalTo(relocatedNote.getId()));
    assertThat(
        relocatedTracker.getRemovedFromTracking(),
        equalTo(associations.relocatedTracker().getRemovedFromTracking()));
    assertThat(
        relocatedTracker.getNextRecallAt(),
        equalTo(associations.relocatedTracker().getNextRecallAt()));
    assertThat(
        memoryTrackerRepository
            .findById(associations.untouchedTracker().getId())
            .orElseThrow()
            .getNote()
            .getId(),
        equalTo(untouchedNote.getId()));
    assertThat(
        mcqRepository.findById(associations.mcqId()).orElseThrow().getNote().getId(),
        equalTo(relocatedNote.getId()));
    assertThat(
        conversationRepository
            .findById(associations.conversationId())
            .orElseThrow()
            .getSubject()
            .getNote()
            .getId(),
        equalTo(relocatedNote.getId()));
  }

  private RelocatedNoteAssociations commitPrivateAssociations(Note relocated, Note untouched) {
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

  private record RelocatedNoteAssociations(
      MemoryTracker relocatedTracker,
      MemoryTracker untouchedTracker,
      Integer mcqId,
      Integer conversationId) {}
}
