package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.controllers.dto.NoteDeleteReferenceHandling;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.services.NoteService;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Verifies that a deleted note continues to reserve its Portable destination. */
class NotebookGitDeletedDestinationControllerTest extends NotebookGitBundleControllerTestBase {

  private static final String PROPOSED_CONTENT = "---\ntype: Note\n---\nProposed content.\n";

  @Autowired NoteService noteService;

  @Test
  void rejectsAnAdditionAtTheDestinationOfADeletedNote() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note deleted =
        inCommittedTransaction(
            transactionManager,
            () -> {
              Notebook persistedNotebook =
                  notebookRepository.findById(notebook.getId()).orElseThrow();
              Note note =
                  makeMe.aNote().notebook(persistedNotebook).title("Reserved destination").please();
              noteService.destroy(
                  note, NoteDeleteReferenceHandling.LEAVE_DEAD_LINKS, currentUser.getUser());
              return note;
            });
    Timestamp deletedAt = deleted.getDeletedAt();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposal =
        proposalBundleBytes(
            binding,
            List.of(new NotebookGitProposalFile("Reserved destination.md", PROPOSED_CONTENT)));

    ApiException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), proposal, ApiException.class);

    assertThat(
        exception.getErrorBody().getMessage(),
        equalTo(
            "A note with this title already exists here but was deleted. Restore the deleted note"
                + " (Undo delete), or choose another title."));
    assertThat(
        exception.getErrorBody().getErrorType(),
        is(ApiError.ErrorType.SOFT_DELETED_TITLE_CONFLICT));
    assertThat(
        exception.getErrorBody().getErrors().get("deletedNoteId"),
        equalTo(String.valueOf(deleted.getId())));

    inCommittedTransaction(
        transactionManager,
        () -> {
          Note persistedDeleted = noteRepository.findById(deleted.getId()).orElseThrow();
          assertThat(persistedDeleted.getDeletedAt(), equalTo(deletedAt));
          assertThat(persistedDeleted.getTitle(), equalTo("Reserved destination"));
          assertThat(persistedDeleted.getFolder(), equalTo(null));
          assertThat(
              noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()), empty());
        });
  }
}
