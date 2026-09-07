package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.exceptions.ApiException;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Verifies that a deleted note continues to reserve its Portable destination. */
class NotebookGitDeletedDestinationControllerTest extends NotebookGitBundleControllerTestBase {

  private static final String ORIGINAL_CONTENT = "---\ntype: Note\n---\nOriginal content.\n";
  private static final String PROPOSED_CONTENT = "---\ntype: Note\n---\nProposed content.\n";
  private static final String SOFT_DELETED_TITLE_REASON =
      "A note with this title already exists here but was deleted. Restore the deleted note"
          + " (Undo delete), or choose another title.";

  @Autowired MemoryTrackerRepository memoryTrackerRepository;

  @Test
  void rejectsASamePathAdditionAfterAcceptedDeletionWithoutResurrectingTheNote() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note reserved =
        makeMe
            .aNote()
            .notebook(notebook)
            .title("Reserved destination")
            .content(ORIGINAL_CONTENT)
            .please();
    Note retained =
        makeMe.aNote().notebook(notebook).title("Retained").content(ORIGINAL_CONTENT).please();
    MemoryTracker tracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(reserved.getId()).orElseThrow())
                    .please());
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] deletionProposal =
        proposalBundleBytes(
            binding, List.of(new NotebookGitProposalFile("Retained.md", ORIGINAL_CONTENT)));

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), deletionProposal);

    PublicationState afterDeletion = publicationState(notebook, reserved, tracker);
    NotebookGitBinding afterDeletionBinding =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
    byte[] additionProposal =
        proposalBundleBytes(
            afterDeletionBinding,
            List.of(
                new NotebookGitProposalFile("Retained.md", ORIGINAL_CONTENT),
                new NotebookGitProposalFile("Reserved destination.md", PROPOSED_CONTENT)));

    ApiException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, afterDeletion.acceptedHead(), additionProposal, ApiException.class);

    assertThat(exception.getErrorBody().getMessage(), containsString("Reserved destination.md"));
    assertThat(exception.getErrorBody().getMessage(), containsString(SOFT_DELETED_TITLE_REASON));
    assertThat(
        exception.getErrorBody().getErrorType(),
        is(ApiError.ErrorType.SOFT_DELETED_TITLE_CONFLICT));
    assertThat(
        exception.getErrorBody().getErrors().get("deletedNoteId"),
        equalTo(String.valueOf(reserved.getId())));
    assertThat(
        exception.getErrorBody().getErrors().get("_originalMessage"),
        equalTo(SOFT_DELETED_TITLE_REASON));
    ApiException original = assertInstanceOf(ApiException.class, exception.getCause());
    assertThat(original.getMessage(), equalTo(SOFT_DELETED_TITLE_REASON));
    assertThat(exception.getErrorBody().getErrors(), equalTo(original.getErrorBody().getErrors()));

    PublicationState afterRejection = publicationState(notebook, reserved, tracker);
    assertThat(afterRejection.noteDeletedAt(), equalTo(afterDeletion.noteDeletedAt()));
    assertThat(afterRejection.trackerDeletedAt(), equalTo(afterDeletion.trackerDeletedAt()));
    inCommittedTransaction(
        transactionManager,
        () ->
            assertThat(
                noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()).stream()
                    .map(Note::getId)
                    .toList(),
                contains(retained.getId())));
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
              reloadedNote.getDeletedAt(),
              reloadedTracker.getDeletedAt());
        });
  }

  private record PublicationState(
      String acceptedHead, Timestamp noteDeletedAt, Timestamp trackerDeletedAt) {}
}
