package com.odde.donut.controllers;

import static com.odde.donut.controllers.NotebookGitRenameScoringBodies.MODERATE_EDIT_BODY;
import static com.odde.donut.controllers.NotebookGitRenameScoringBodies.SUBSTANTIAL_ORIGINAL_BODY;
import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.entities.Conversation;
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

/** Preserves private learning and conversation identities across accepted Git renames. */
class NotebookGitProposalRenamePrivateAssociationControllerTest
    extends NotebookGitControllerTestBase {
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
  void acceptsASameParentRenameWithEditedContentRetainingNoteAndLearningIdentities()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note =
        makeMe.aNote().notebook(notebook).title("note").content(SUBSTANTIAL_ORIGINAL_BODY).please();
    MemoryTracker tracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(note.getId()).orElseThrow())
                    .nextRecallAt(makeMe.aTimestamp().of(5, 0).please())
                    .please());
    RenamedNoteAssociations associations =
        inCommittedTransaction(
            transactionManager,
            () -> {
              Note reloaded = noteRepository.findById(note.getId()).orElseThrow();
              Mcq mcq = makeMe.anMcq().forNote(reloaded).please();
              Conversation conversation =
                  makeMe.aConversation().forANote(reloaded).from(currentUser.getUser()).please();
              conversationId = conversation.getId();
              return new RenamedNoteAssociations(mcq.getId(), conversation.getId());
            });
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding, List.of(new NotebookGitProposalFile("renamed.md", MODERATE_EDIT_BODY)));

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    Note renamed = noteRepository.findById(note.getId()).orElseThrow();
    assertThat(renamed.getId(), equalTo(note.getId()));
    assertThat(renamed.getTitle(), equalTo("renamed"));
    assertThat(renamed.getContent(), equalTo(MODERATE_EDIT_BODY));
    MemoryTracker reloadedTracker = memoryTrackerRepository.findById(tracker.getId()).orElseThrow();
    assertThat(reloadedTracker.getNote().getId(), equalTo(note.getId()));
    assertThat(reloadedTracker.getNextRecallAt(), equalTo(tracker.getNextRecallAt()));
    assertThat(
        mcqRepository.findById(associations.mcqId()).orElseThrow().getNote().getId(),
        equalTo(note.getId()));
    assertThat(
        conversationRepository
            .findById(associations.conversationId())
            .orElseThrow()
            .getSubject()
            .getNote()
            .getId(),
        equalTo(note.getId()));
  }

  @Test
  void preservesPrivateAssociationsOfTheRenamedNoteAndLeavesTheOtherIdenticalTextNoteUntouched()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note renamedNote =
        makeMe.aNote().notebook(notebook).title("Original").content(MATCHING_CONTENT).please();
    Note untouchedNote =
        makeMe.aNote().notebook(notebook).title("Untouched").content(MATCHING_CONTENT).please();

    MemoryTracker trackerForRenamed =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(renamedNote.getId()).orElseThrow())
                    .removedFromTracking()
                    .nextRecallAt(makeMe.aTimestamp().of(5, 0).please())
                    .please());
    MemoryTracker trackerForUntouched =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(untouchedNote.getId()).orElseThrow())
                    .nextRecallAt(makeMe.aTimestamp().of(10, 0).please())
                    .please());
    RenamedNoteAssociations associations =
        inCommittedTransaction(
            transactionManager,
            () -> {
              Note reloadedRenamed = noteRepository.findById(renamedNote.getId()).orElseThrow();
              Mcq mcq = makeMe.anMcq().forNote(reloadedRenamed).please();
              Conversation conversation =
                  makeMe
                      .aConversation()
                      .forANote(reloadedRenamed)
                      .from(currentUser.getUser())
                      .please();
              conversationId = conversation.getId();
              return new RenamedNoteAssociations(mcq.getId(), conversation.getId());
            });

    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("renamed.md", MATCHING_CONTENT),
                new NotebookGitProposalFile("Untouched.md", MATCHING_CONTENT)));

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    Note reloadedRenamed = noteRepository.findById(renamedNote.getId()).orElseThrow();
    assertThat(reloadedRenamed.getId(), equalTo(renamedNote.getId()));
    assertThat(reloadedRenamed.getTitle(), equalTo("renamed"));
    Note reloadedUntouched = noteRepository.findById(untouchedNote.getId()).orElseThrow();
    assertThat(reloadedUntouched.getId(), equalTo(untouchedNote.getId()));
    assertThat(reloadedUntouched.getTitle(), equalTo("Untouched"));

    MemoryTracker reloadedTrackerForRenamed =
        memoryTrackerRepository.findById(trackerForRenamed.getId()).orElseThrow();
    assertThat(reloadedTrackerForRenamed.getNote().getId(), equalTo(renamedNote.getId()));
    assertThat(
        reloadedTrackerForRenamed.getRemovedFromTracking(),
        equalTo(trackerForRenamed.getRemovedFromTracking()));
    assertThat(
        reloadedTrackerForRenamed.getNextRecallAt(), equalTo(trackerForRenamed.getNextRecallAt()));

    MemoryTracker reloadedTrackerForUntouched =
        memoryTrackerRepository.findById(trackerForUntouched.getId()).orElseThrow();
    assertThat(reloadedTrackerForUntouched.getNote().getId(), equalTo(untouchedNote.getId()));
    assertThat(
        reloadedTrackerForUntouched.getRemovedFromTracking(),
        equalTo(trackerForUntouched.getRemovedFromTracking()));
    assertThat(
        reloadedTrackerForUntouched.getNextRecallAt(),
        equalTo(trackerForUntouched.getNextRecallAt()));

    Mcq mcq = mcqRepository.findById(associations.mcqId()).orElseThrow();
    assertThat(mcq.getNote().getId(), equalTo(renamedNote.getId()));
    Conversation conversation =
        conversationRepository.findById(associations.conversationId()).orElseThrow();
    assertThat(conversation.getSubject().getNote().getId(), equalTo(renamedNote.getId()));
  }

  private record RenamedNoteAssociations(Integer mcqId, Integer conversationId) {}
}
