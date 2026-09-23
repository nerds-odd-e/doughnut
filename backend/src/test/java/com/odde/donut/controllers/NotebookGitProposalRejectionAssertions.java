package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.testability.GitBundleTestReader.AcceptedHistory;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Asserts proposal rejection without mutating the notebook's accepted Git binding. */
final class NotebookGitProposalRejectionAssertions {

  @FunctionalInterface
  interface AcceptedHistoryLookup {
    AcceptedHistory lookup(Notebook notebook) throws Exception;
  }

  private NotebookGitProposalRejectionAssertions() {}

  static ResponseStatusException assertRejectedWithoutMutatingBinding(
      NotebookController controller,
      NotebookGitBindingRepository notebookGitBindingRepository,
      AcceptedHistoryLookup acceptedHistory,
      Notebook notebook,
      String expectedHead,
      byte[] bundleBytes,
      HttpStatus expectedStatus)
      throws Exception {
    ResponseStatusException exception =
        assertRejectedWithoutMutatingBinding(
            controller,
            notebookGitBindingRepository,
            acceptedHistory,
            notebook,
            expectedHead,
            bundleBytes,
            ResponseStatusException.class);

    assertThat(exception.getStatusCode(), equalTo(expectedStatus));
    return exception;
  }

  static <T extends RuntimeException> T assertRejectedWithoutMutatingBinding(
      NotebookController controller,
      NotebookGitBindingRepository notebookGitBindingRepository,
      AcceptedHistoryLookup acceptedHistory,
      Notebook notebook,
      String expectedHead,
      byte[] bundleBytes,
      Class<T> exceptionType)
      throws Exception {
    NotebookGitBinding before =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
    String acceptedHeadBefore = before.getAcceptedGitObjectId();
    AcceptedHistory acceptedHistoryBefore = acceptedHistory.lookup(notebook);
    Instant updatedAtBefore = before.getUpdatedAt().toInstant();

    T exception =
        assertThrows(
            exceptionType,
            () ->
                controller.publishNotebookGitProposal(notebook.getId(), expectedHead, bundleBytes));

    NotebookGitBinding after =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
    assertThat(after.getAcceptedGitObjectId(), equalTo(acceptedHeadBefore));
    assertThat(acceptedHistory.lookup(notebook), equalTo(acceptedHistoryBefore));
    assertThat(after.getUpdatedAt().toInstant(), equalTo(updatedAtBefore));
    return exception;
  }
}
