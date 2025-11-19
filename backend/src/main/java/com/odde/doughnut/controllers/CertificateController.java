package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Certificate;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.AuthorizationService;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/certificate")
public class CertificateController {

  private final UserModel currentUser;

  private final ModelFactoryService modelFactoryService;
  private final AuthorizationService authorizationService;

  public CertificateController(
      UserModel currentUser,
      ModelFactoryService modelFactoryService,
      AuthorizationService authorizationService) {
    this.currentUser = currentUser;
    this.modelFactoryService = modelFactoryService;
    this.authorizationService = authorizationService;
  }

  @PostMapping("/{notebook}")
  @Transactional
  public Certificate claimCertificate(@PathVariable @Schema(type = "integer") Notebook notebook) {
    authorizationService.assertLoggedIn(currentUser.getEntity());
    return modelFactoryService.certificateRepository.findFirstByUserAndNotebook(
        currentUser.getEntity(), notebook);
  }

  @GetMapping("/{notebook}")
  public Certificate getCertificate(@PathVariable @Schema(type = "integer") Notebook notebook) {
    return modelFactoryService.certificateRepository.findFirstByUserAndNotebook(
        currentUser.getEntity(), notebook);
  }
}
