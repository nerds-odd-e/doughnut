package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Certificate;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.models.TimestampOperations;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Resource;
import java.sql.Timestamp;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/certificate")
public class RestCertificateController {

  private final UserModel currentUser;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  public RestCertificateController(UserModel currentUser, TestabilitySettings testabilitySettings) {
    this.currentUser = currentUser;
    this.testabilitySettings = testabilitySettings;
  }

  @PostMapping("/{notebook}")
  public Certificate saveCertificate(@PathVariable @Schema(type = "integer") Notebook notebook) {
    Certificate certificate = new Certificate();
    certificate.setUser(this.currentUser.getEntity());
    certificate.setNotebook(notebook);
    // Set start date to current time
    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
    certificate.setStartDate(now);
    // Hard code expiry date to 1 year from current time
    certificate.setExpiryDate(TimestampOperations.addHoursToTimestamp(now, 8760));
    return certificate;
  }

  @GetMapping("/{notebook}")
  public Certificate getCertificate(@PathVariable @Schema(type = "integer") Notebook notebook) {
    Certificate certificate = new Certificate();
    certificate.setUser(this.currentUser.getEntity());
    certificate.setNotebook(notebook);
    // Set start date to current time
    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
    certificate.setStartDate(now);
    // Hard code expiry date to 1 year from current time
    certificate.setExpiryDate(TimestampOperations.addHoursToTimestamp(now, 8760));
    return certificate;
  }
}
