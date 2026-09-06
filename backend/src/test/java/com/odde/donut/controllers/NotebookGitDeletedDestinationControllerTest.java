package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.controllers.dto.NoteDeleteReferenceHandling;
import com.odde.donut.entities.Folder;
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
  void identifiesADeletedDestinationAndRejectsTheWholeMixedProposal() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    String originalContent = "---\ntype: Note\n---\nOriginal content.\n";
    Note existing =
        makeMe.aNote().notebook(notebook).title("Existing").content(originalContent).please();
    Folder physics = makeMe.aFolder().notebook(notebook).name("Physics").please();
    Note motion = makeMe.aNote().folder(physics).title("Motion").content(originalContent).please();
    Note deleted =
        inCommittedTransaction(
            transactionManager,
            () -> {
              Note note =
                  makeMe
                      .aNote()
                      .folder(entityManager.find(Folder.class, physics.getId()))
                      .title("Reserved destination")
                      .content(originalContent)
                      .please();
              noteService.destroy(
                  note, NoteDeleteReferenceHandling.LEAVE_DEAD_LINKS, currentUser.getUser());
              return note;
            });
    Timestamp deletedAt = deleted.getDeletedAt();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    Timestamp existingUpdatedAt =
        inCommittedTransaction(
            transactionManager,
            () -> noteRepository.findById(existing.getId()).orElseThrow().getUpdatedAt());
    byte[] proposal =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Added.md", PROPOSED_CONTENT),
                new NotebookGitProposalFile("Existing.md", PROPOSED_CONTENT),
                new NotebookGitProposalFile("Physics/Motion.md", originalContent),
                new NotebookGitProposalFile("Physics/Reserved destination.md", PROPOSED_CONTENT)));

    ApiException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), proposal, ApiException.class);

    assertThat(
        exception.getErrorBody().getMessage(), containsString("Physics/Reserved destination.md"));
    String reason =
        "A note with this title already exists here but was deleted. Restore the deleted note"
            + " (Undo delete), or choose another title.";
    assertThat(exception.getErrorBody().getMessage(), containsString(reason));
    assertThat(
        exception.getErrorBody().getErrorType(),
        is(ApiError.ErrorType.SOFT_DELETED_TITLE_CONFLICT));
    assertThat(
        exception.getErrorBody().getErrors().get("deletedNoteId"),
        equalTo(String.valueOf(deleted.getId())));
    assertThat(exception.getErrorBody().getErrors().get("_originalMessage"), equalTo(reason));
    ApiException original = assertInstanceOf(ApiException.class, exception.getCause());
    assertThat(original.getMessage(), equalTo(reason));
    assertThat(exception.getErrorBody().getErrors(), equalTo(original.getErrorBody().getErrors()));

    inCommittedTransaction(
        transactionManager,
        () -> {
          Note persistedDeleted = noteRepository.findById(deleted.getId()).orElseThrow();
          assertThat(persistedDeleted.getDeletedAt(), equalTo(deletedAt));
          assertThat(persistedDeleted.getTitle(), equalTo("Reserved destination"));
          assertThat(persistedDeleted.getContent(), equalTo(originalContent));
          assertThat(persistedDeleted.getFolder().getId(), equalTo(physics.getId()));
          Note persistedExisting = noteRepository.findById(existing.getId()).orElseThrow();
          assertThat(persistedExisting.getContent(), equalTo(originalContent));
          assertThat(persistedExisting.getUpdatedAt(), equalTo(existingUpdatedAt));
          assertThat(
              noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()).stream()
                  .map(Note::getId)
                  .toList(),
              contains(existing.getId(), motion.getId()));
        });
  }
}
