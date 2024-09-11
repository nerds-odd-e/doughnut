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
@RequestMapping("/api/notebooks")
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

  @PostMapping(value = "/{notebook}/request-approval")
  @Transactional
  public Notebook requestNotebookApproval(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(notebook);
    modelFactoryService.notebookService(notebook).requestNotebookApproval();
    return notebook;
  }

  @GetMapping("/getAllPendingRequestNoteBooks")
  public List<NotebookCertificateApproval> getAllPendingRequestNotebooks()
      throws UnexpectedNoAccessRightException {
    currentUser.assertAdminAuthorization();
    return modelFactoryService.notebookCertificateApprovalRepository.findByLastApprovalTimeIsNull();
  }

  @PostMapping(value = "/{notebook}/approve")
  @Transactional
  public Notebook approveNoteBook(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAdminAuthorization();
    Iterable<NotebookCertificateApproval> all =
        modelFactoryService.notebookCertificateApprovalRepository.findAll();
    for (NotebookCertificateApproval approval : all) {
      if (approval.getNotebook().getId().equals(notebook.getId())) {
        approval.setLastApprovalTime(testabilitySettings.getCurrentUTCTimestamp());
        modelFactoryService.save(notebook);
      }
    }
    return notebook;
  }
}
