package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.NotebookCertificateApprovalService;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RestNotebookCertificateApprovalControllerTest {
  @Autowired ModelFactoryService modelFactoryService;

  @Autowired MakeMe makeMe;
  private UserModel userModel;
  RestNotebookCertificateApprovalController controller;
  private TestabilitySettings testabilitySettings = new TestabilitySettings();

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
    controller =
        new RestNotebookCertificateApprovalController(
            modelFactoryService, userModel, testabilitySettings);
  }

  @Nested
  class requestNotebookApproval {
    @Test
    void shouldNotBeAbleToRequestApprovalForNotebookThatBelongsToOtherUser() {
      User anotherUser = makeMe.aUser().please();
      Note note = makeMe.aNote().creatorAndOwner(anotherUser).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.requestNotebookApproval(note.getNotebook()));
    }

    @Test
    void approvalStatusShouldBePendingAfterRequestingApproval()
        throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().creatorAndOwner(userModel).please();
      controller.requestNotebookApproval(note.getNotebook());
      makeMe.refresh(note.getNotebook());
      assertThat(note.getNotebook().getApprovalStatus(), equalTo(ApprovalStatus.PENDING));
    }
  }

  @Nested
  class getAllPendingRequestNotebooks {
    private Notebook notebook;
    private NotebookCertificateApprovalService approval;

    @BeforeEach
    void setup() {
      UserModel userModel = makeMe.anAdmin().toModelPlease();
      notebook = makeMe.aNote().creatorAndOwner(userModel).please().getNotebook();
      controller =
          new RestNotebookCertificateApprovalController(
              modelFactoryService, userModel, testabilitySettings);
      approval = makeMe.modelFactoryService.notebookService(notebook).requestNotebookApproval();
      makeMe.refresh(notebook);
    }

    @Test
    void shouldReturnPendingRequestNotebooks() throws UnexpectedNoAccessRightException {
      var result = controller.getAllPendingRequestNotebooks();
      assertThat(result, hasSize(1));
    }

    @Test
    void shouldNotReturnApprovedNotebooks() throws UnexpectedNoAccessRightException {
      approval.approve(makeMe.aTimestamp().please());
      makeMe.refresh(notebook);
      var result = controller.getAllPendingRequestNotebooks();
      assertThat(result, hasSize(0));
    }

    @Test
    void shouldApproveNoteBook() throws UnexpectedNoAccessRightException {
      Notebook result = controller.approveNoteBook(notebook);
      assertThat(result.getApprovalStatus(), equalTo(ApprovalStatus.APPROVED));
    }
  }
}
