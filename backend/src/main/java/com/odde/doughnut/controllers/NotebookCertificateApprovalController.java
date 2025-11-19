package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.NotebookCertificateApprovalService;
import com.odde.doughnut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notebook_certificate_approvals")
class NotebookCertificateApprovalController {
  private final ModelFactoryService modelFactoryService;
  private CurrentUser currentUser;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  private final AuthorizationService authorizationService;

  public NotebookCertificateApprovalController(
      ModelFactoryService modelFactoryService,
      CurrentUser currentUser,
      TestabilitySettings testabilitySettings,
      AuthorizationService authorizationService) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
    this.testabilitySettings = testabilitySettings;
    this.authorizationService = authorizationService;
  }

  @GetMapping("/for-notebook/{notebook}")
  public NotebookCertificateApproval getApprovalForNotebook(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(currentUser.getUser(), notebook);
    return notebook.getNotebookCertificateApproval();
  }

  @PostMapping(value = "/request-approval/{notebook}")
  @Transactional
  public NotebookCertificateApproval requestApprovalForNotebook(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(currentUser.getUser(), notebook);
    return modelFactoryService.notebookService(notebook).requestNotebookApproval().getApproval();
  }

  @GetMapping("/get-all-pending-request")
  public List<NotebookCertificateApproval> getAllPendingRequest()
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAdminAuthorization(currentUser.getUser());
    return modelFactoryService.notebookCertificateApprovalRepository.findByLastApprovalTimeIsNull();
  }

  @PostMapping(value = "/{notebookCertificateApproval}/approve")
  @Transactional
  public NotebookCertificateApproval approve(
      @PathVariable("notebookCertificateApproval") @Schema(type = "integer")
          NotebookCertificateApproval approval)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAdminAuthorization(currentUser.getUser());
    new NotebookCertificateApprovalService(approval, modelFactoryService)
        .approve(testabilitySettings.getCurrentUTCTimestamp());
    return approval;
  }
}
