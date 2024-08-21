package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.SaveCertificateDetails;
import com.odde.doughnut.entities.Certificate;
import com.odde.doughnut.models.UserModel;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/certificate")
public class RestCertificateController {

  private final UserModel currentUser;

  public RestCertificateController(UserModel currentUser) {
    this.currentUser = currentUser;
  }

  @PostMapping
  // Return an empty cert with current user
  public Certificate saveCertificate(@RequestBody SaveCertificateDetails certificateDetails) {
    Certificate certificate = new Certificate();
    certificate.setUser(this.currentUser.getEntity());
    certificate.setNotebook(certificateDetails.getNotebook());
    return certificate;
  }
}
