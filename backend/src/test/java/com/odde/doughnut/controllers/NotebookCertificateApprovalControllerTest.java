package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.NotebookCertificateApprovalService;
import com.odde.doughnut.services.NotebookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NotebookCertificateApprovalControllerTest extends ControllerTestBase {
  @Autowired NotebookService notebookService;
  @Autowired NotebookCertificateApprovalService notebookCertificateApprovalService;
  @Autowired NotebookCertificateApprovalController controller;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Nested
  class getNotebookApproval {
    @Test
    void shouldNotBeAbleToGetApprovalForNotebookThatBelongsToOtherUser() {
      User anotherUser = makeMe.aUser().please();
      Note note = makeMe.aNote().creatorAndOwner(anotherUser).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.getApprovalForNotebook(note.getNotebook()));
    }

    @Test
    void approvalStatusShouldBeNullIfNotExist() throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      NotebookCertificateApproval approvalForNotebook =
          controller.getApprovalForNotebook(note.getNotebook());
      assertThat(approvalForNotebook, nullValue());
    }

    @Test
    void success() throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      notebookService.requestNotebookApproval(note.getNotebook());
      makeMe.refresh(note.getNotebook());
      NotebookCertificateApproval approvalForNotebook =
          controller.getApprovalForNotebook(note.getNotebook());
      assertThat(approvalForNotebook.getNotebook(), equalTo(note.getNotebook()));
    }
  }

  @Nested
  class requestNotebookApproval {
    @Test
    void shouldNotBeAbleToRequestApprovalForNotebookThatBelongsToOtherUser() {
      User anotherUser = makeMe.aUser().please();
      Note note = makeMe.aNote().creatorAndOwner(anotherUser).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.requestApprovalForNotebook(note.getNotebook()));
    }

    @Test
    void approvalStatusShouldBePendingAfterRequestingApproval()
        throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      controller.requestApprovalForNotebook(note.getNotebook());
      makeMe.refresh(note.getNotebook());
      assertFalse(note.getNotebook().isCertifiable());
    }
  }

  @Nested
  class getAllPendingRequestNotebooks {
    private Notebook notebook;
    private NotebookCertificateApproval approval;

    @BeforeEach
    void setup() {
      currentUser.setUser(makeMe.anAdmin().please());
      notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      approval = notebookService.requestNotebookApproval(notebook);
      makeMe.refresh(notebook);
    }

    @Test
    void shouldReturnPendingRequestNotebooks() throws UnexpectedNoAccessRightException {
      var result = controller.getAllPendingRequest();
      assertThat(result, hasSize(1));
    }

    @Test
    void shouldNotReturnApprovedNotebooks() throws UnexpectedNoAccessRightException {
      notebookCertificateApprovalService.approve(approval, makeMe.aTimestamp().please());
      makeMe.refresh(notebook);
      var result = controller.getAllPendingRequest();
      assertThat(result, hasSize(0));
    }

    @Test
    void shouldApproveNoteBook() throws UnexpectedNoAccessRightException {
      Notebook result = controller.approve(approval).getNotebook();
      assertTrue(result.isCertifiable());
    }
  }
}
