package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Certificate;
import com.odde.doughnut.models.UserModel;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/certificate")
public class RestCertificateController {

  private final UserModel currentUser;

  public RestCertificateController(UserModel currentUser) {
    this.currentUser = currentUser;
  }

  // Return an empty cert with current user
  public Certificate saveCertificate() {
    Certificate certificate = new Certificate();
    certificate.setUser(this.currentUser.getEntity());
    return certificate;
  }
}
