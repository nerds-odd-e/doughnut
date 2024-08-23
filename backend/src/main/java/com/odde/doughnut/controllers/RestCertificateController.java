package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Certificate;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Resource;
import java.sql.Timestamp;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/certificate")
public class RestCertificateController {

  public static final int oneYearInHours = 8760;
  private final UserModel currentUser;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  private final ModelFactoryService modelFactoryService;

  public RestCertificateController(
      UserModel currentUser,
      TestabilitySettings testabilitySettings,
      ModelFactoryService modelFactoryService) {
    this.currentUser = currentUser;
    this.testabilitySettings = testabilitySettings;
    this.modelFactoryService = modelFactoryService;
  }

  @PostMapping("/{notebook}")
  @Transactional
  public Certificate saveCertificate(@PathVariable @Schema(type = "integer") Notebook notebook) {
    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
    Timestamp expiryDate =
        Timestamp.valueOf(
            now.toLocalDateTime().plus(notebook.getNotebookSettings().getCertificateExpiry()));

    Certificate old_cert =
        modelFactoryService.certificateRepository.findFirstByUserAndNotebook(
            currentUser.getEntity(), notebook);
    if (old_cert != null) {
      old_cert.setExpiryDate(expiryDate);
      modelFactoryService.save(old_cert);
      return old_cert;
    }

    Certificate certificate = new Certificate();
    certificate.setUser(this.currentUser.getEntity());
    certificate.setNotebook(notebook);
    certificate.setExpiryDate(expiryDate);
    certificate.setStartDate(now);
    modelFactoryService.save(certificate);
    return certificate;
  }

  @GetMapping("/{notebook}")
  public Certificate getCertificate(@PathVariable @Schema(type = "integer") Notebook notebook) {
    return modelFactoryService.certificateRepository.findFirstByUserAndNotebook(
        currentUser.getEntity(), notebook);
  }
}
