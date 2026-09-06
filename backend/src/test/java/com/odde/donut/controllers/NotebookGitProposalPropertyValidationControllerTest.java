package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.algorithms.FrontmatterNoteLevel;
import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.exceptions.ApiException;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Verifies {@code publishNotebookGitProposal} reuses the existing authored-property validation
 * contract ({@link com.odde.donut.validators.AuthoredNoteContent#assertValidForSave}) for each
 * changed note, once the proposal clears the tree-shape and typed-Markdown gates.
 */
class NotebookGitProposalPropertyValidationControllerTest
    extends NotebookGitBundleControllerTestBase {

  @Test
  void identifiesALaterInvalidPropertyAndRejectsTheWholeMixedProposal() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    String originalContent = "---\ntype: Note\n---\noriginal content";
    Note existing =
        makeMe.aNote().notebook(notebook).title("Existing").content(originalContent).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposal =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Added.md", originalContent),
                new NotebookGitProposalFile("Existing.md", "---\ntype: Note\n---\nchanged content"),
                new NotebookGitProposalFile(
                    "Invalid.md", "---\ntype: Note\nnote_level: 7\n---\ninvalid content")));

    ApiException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), proposal, ApiException.class);

    assertThat(exception.getErrorBody().getMessage(), containsString("Invalid.md"));
    assertThat(
        exception.getErrorBody().getMessage(),
        containsString(FrontmatterNoteLevel.AUTHORED_NOTE_LEVEL_MESSAGE));
    assertThat(exception.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.BINDING_ERROR));
    assertThat(
        exception.getErrorBody().getErrors().get("note_level"),
        equalTo(FrontmatterNoteLevel.AUTHORED_NOTE_LEVEL_MESSAGE));
    ApiException original = assertInstanceOf(ApiException.class, exception.getCause());
    assertThat(exception.getErrorBody().getErrors(), equalTo(original.getErrorBody().getErrors()));
    inCommittedTransaction(
        transactionManager,
        () -> {
          assertThat(
              noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()), hasSize(1));
          assertThat(
              noteRepository.findById(existing.getId()).orElseThrow().getContent(),
              equalTo(originalContent));
        });
  }

  @Test
  void rejectsAnInvalidNoteLevelWithTheSameActionablePropertyErrorWithoutMutatingTheBinding()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe
        .aNote()
        .notebook(notebook)
        .title("note")
        .content("---\ntype: Note\n---\noriginal content")
        .please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    NotebookGitBinding before =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
    byte[] bundleBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile(
                    "note.md", "---\ntype: Note\nnote_level: 7\n---\nchanged content")));

    ApiException exception =
        assertThrows(
            ApiException.class,
            () ->
                controller.publishNotebookGitProposal(
                    notebook.getId(), binding.getAcceptedGitObjectId(), bundleBytes));

    assertThat(exception.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.BINDING_ERROR));
    assertThat(
        exception.getErrorBody().getErrors().get("note_level"),
        equalTo(FrontmatterNoteLevel.AUTHORED_NOTE_LEVEL_MESSAGE));

    NotebookGitBinding after =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
    assertThat(after.getAcceptedGitObjectId(), equalTo(before.getAcceptedGitObjectId()));
    assertThat(after.getBundleBytes(), equalTo(before.getBundleBytes()));
    assertThat(after.getUpdatedAt(), equalTo(before.getUpdatedAt()));
  }
}
