package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.NotebookCertificateApprovalRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.NotebookCertificateApprovalService;
import com.odde.doughnut.services.NotebookService;
import com.odde.doughnut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notebook_certificate_approvals")
class NotebookCertificateApprovalController {
  private final NotebookService notebookService;
  private final NotebookCertificateApprovalRepository notebookCertificateApprovalRepository;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  private final AuthorizationService authorizationService;
  private final EntityManager entityManager;

  public NotebookCertificateApprovalController(
      NotebookService notebookService,
      NotebookCertificateApprovalRepository notebookCertificateApprovalRepository,
      TestabilitySettings testabilitySettings,
      AuthorizationService authorizationService,
      EntityManager entityManager) {
    this.notebookService = notebookService;
    this.notebookCertificateApprovalRepository = notebookCertificateApprovalRepository;
    this.testabilitySettings = testabilitySettings;
    this.authorizationService = authorizationService;
    this.entityManager = entityManager;
  }

  @GetMapping("/for-notebook/{notebook}")
  public NotebookCertificateApproval getApprovalForNotebook(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(notebook);
    return notebook.getNotebookCertificateApproval();
  }

  @PostMapping(value = "/request-approval/{notebook}")
  @Transactional
  public NotebookCertificateApproval requestApprovalForNotebook(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(notebook);
    return notebookService.requestNotebookApproval(notebook).getApproval();
  }

  @GetMapping("/get-all-pending-request")
  public List<NotebookCertificateApproval> getAllPendingRequest()
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAdminAuthorization();
    return notebookCertificateApprovalRepository.findByLastApprovalTimeIsNull();
  }

  @PostMapping(value = "/{notebookCertificateApproval}/approve")
  @Transactional
  public NotebookCertificateApproval approve(
      @PathVariable("notebookCertificateApproval") @Schema(type = "integer")
          NotebookCertificateApproval approval)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAdminAuthorization();
    new NotebookCertificateApprovalService(approval, entityManager)
        .approve(testabilitySettings.getCurrentUTCTimestamp());
    return approval;
  }
}
