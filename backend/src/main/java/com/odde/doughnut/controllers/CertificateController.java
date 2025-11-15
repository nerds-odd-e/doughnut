package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Certificate;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/certificate")
public class CertificateController {

  private final UserModel currentUser;

  private final ModelFactoryService modelFactoryService;

  public CertificateController(UserModel currentUser, ModelFactoryService modelFactoryService) {
    this.currentUser = currentUser;
    this.modelFactoryService = modelFactoryService;
  }

  @PostMapping("/{notebook}")
  @Transactional
  public Certificate claimCertificate(@PathVariable @Schema(type = "integer") Notebook notebook) {
    currentUser.assertLoggedIn();
    return modelFactoryService.certificateRepository.findFirstByUserAndNotebook(
        currentUser.getEntity(), notebook);
  }

  @GetMapping("/{notebook}")
  public Certificate getCertificate(@PathVariable @Schema(type = "integer") Notebook notebook) {
    return modelFactoryService.certificateRepository.findFirstByUserAndNotebook(
        currentUser.getEntity(), notebook);
  }
}
