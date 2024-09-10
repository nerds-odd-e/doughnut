package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Certificate;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.AssessmentService;
import com.odde.doughnut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/certificate")
public class RestCertificateController {

  private final UserModel currentUser;

  private final ModelFactoryService modelFactoryService;

  private final AssessmentService assessmentService;

  public RestCertificateController(
      UserModel currentUser,
      TestabilitySettings testabilitySettings,
      ModelFactoryService modelFactoryService) {
    this.currentUser = currentUser;
    this.modelFactoryService = modelFactoryService;
    this.assessmentService = new AssessmentService(modelFactoryService, testabilitySettings);
  }

  @PostMapping("/{notebook}")
  @Transactional
  public Certificate claimCertificate(@PathVariable @Schema(type = "integer") Notebook notebook) {
    currentUser.assertLoggedIn();
    return assessmentService.claimCertificateForPassedAssessment(notebook, currentUser.getEntity());
  }

  @GetMapping("/{notebook}")
  public Certificate getCertificate(@PathVariable @Schema(type = "integer") Notebook notebook) {
    return modelFactoryService.certificateRepository.findFirstByUserAndNotebook(
        currentUser.getEntity(), notebook);
  }
}
