package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notebook_certificate_approvals")
class RestNotebookCertificateApprovalController {
  private final ModelFactoryService modelFactoryService;
  private UserModel currentUser;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  public RestNotebookCertificateApprovalController(
      ModelFactoryService modelFactoryService,
      UserModel currentUser,
      TestabilitySettings testabilitySettings) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
    this.testabilitySettings = testabilitySettings;
  }

  @GetMapping("/for-notebook/{notebook}")
  public NotebookCertificateApproval getApprovalForNotebook(      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
    throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(notebook);
    return notebook.getNotebookCertificateApproval();
  }

  @PostMapping(value = "/request-approval/{notebook}")
  @Transactional
  public NotebookCertificateApproval requestApprovalForNotebook(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(notebook);
    return modelFactoryService.notebookService(notebook).requestNotebookApproval().getApproval();
  }

  @GetMapping("/get-all-pending-request")
  public List<NotebookCertificateApproval> getAllPendingRequest()
      throws UnexpectedNoAccessRightException {
    currentUser.assertAdminAuthorization();
    return modelFactoryService.notebookCertificateApprovalRepository.findByLastApprovalTimeIsNull();
  }

  @PostMapping(value = "/{notebookCertificateApproval}/approve")
  @Transactional
  public NotebookCertificateApproval approve(
      @PathVariable("notebookCertificateApproval") @Schema(type = "integer")
          NotebookCertificateApproval approval)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAdminAuthorization();
    approval.setLastApprovalTime(testabilitySettings.getCurrentUTCTimestamp());
    return approval;
  }
}
