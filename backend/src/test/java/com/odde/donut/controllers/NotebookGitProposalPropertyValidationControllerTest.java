package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.algorithms.FrontmatterNoteLevel;
import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.exceptions.ApiException;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Verifies {@code publishNotebookGitProposal} reuses the existing authored-property validation
 * contract ({@link com.odde.donut.validators.AuthoredNoteContent#assertValidForSave}) for the one
 * changed note, once the proposal clears the tree-shape and typed-Markdown gates.
 */
class NotebookGitProposalPropertyValidationControllerTest
    extends NotebookGitBundleControllerTestBase {

  @Test
  void rejectsAnInvalidNoteLevelWithTheSameActionablePropertyErrorWithoutMutatingTheBinding()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = seedAcceptedBinding(notebook, validBaselineEntries());
    NotebookGitBinding before =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
    byte[] bundleBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new ProposedFile("note.md", "---\ntype: Note\nnote_level: 7\n---\nchanged content"),
                new ProposedFile("README.md", "---\ntype: Readme\n---\nreadme original")));

    ApiException exception =
        assertThrows(
            ApiException.class,
            () ->
                controller.publishNotebookGitProposal(
                    notebook, binding.getAcceptedGitObjectId(), bundleBytes));

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
