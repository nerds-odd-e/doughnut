package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Certificate;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.repositories.CertificateRepository;
import com.odde.doughnut.services.AuthorizationService;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/certificate")
public class CertificateController {

  private final CertificateRepository certificateRepository;
  private final AuthorizationService authorizationService;

  public CertificateController(
      CertificateRepository certificateRepository, AuthorizationService authorizationService) {
    this.certificateRepository = certificateRepository;
    this.authorizationService = authorizationService;
  }

  @PostMapping("/{notebook}")
  @Transactional
  public Certificate claimCertificate(@PathVariable @Schema(type = "integer") Notebook notebook) {
    authorizationService.assertLoggedIn();
    return certificateRepository.findFirstByUserAndNotebook(
        authorizationService.getCurrentUser(), notebook);
  }

  @GetMapping("/{notebook}")
  public Certificate getCertificate(@PathVariable @Schema(type = "integer") Notebook notebook) {
    return certificateRepository.findFirstByUserAndNotebook(
        authorizationService.getCurrentUser(), notebook);
  }
}
