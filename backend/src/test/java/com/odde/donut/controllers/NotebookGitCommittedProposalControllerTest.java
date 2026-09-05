package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class NotebookGitCommittedProposalControllerTest
    extends NotebookGitCommittedProposalControllerTestBase {

  @Test
  void committedValidProposalStillReachesTheInterimRefusal() throws Exception {
    CommittedProposalFixture fixture = createCommittedProposalFixture();

    ResponseStatusException exception =
        inRequestContextWorker(
            () -> {
              Notebook notebook = reloadNotebook(fixture.notebookId());
              return assertThrows(
                  ResponseStatusException.class,
                  () ->
                      controller.publishNotebookGitProposal(
                          notebook, fixture.expectedHead(), fixture.proposal()));
            });

    assertThat(exception.getStatusCode(), equalTo(HttpStatus.NOT_IMPLEMENTED));
    NotebookGitBinding reloadedBinding = reloadBinding(fixture.notebookId());
    assertThat(reloadedBinding.getAcceptedGitObjectId(), equalTo(fixture.expectedHead()));
  }
}
