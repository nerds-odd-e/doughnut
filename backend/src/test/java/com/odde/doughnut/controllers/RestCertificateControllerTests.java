package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.odde.doughnut.controllers.dto.SaveCertificateDetails;
import com.odde.doughnut.entities.Certificate;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class RestCertificateControllerTests {
  @Autowired MakeMe makeMe;
  private UserModel currentUser;
  private RestCertificateController controller;

  @BeforeEach
  void setup() {
    currentUser = makeMe.aUser().toModelPlease();
    controller = new RestCertificateController(currentUser);
  }

  // Create a nested test class for the getCertificate method
  @Nested
  class SaveCertificate {

    private Notebook notebook;
    private SaveCertificateDetails saveCertificate;

    @BeforeEach
    void setup() {
      notebook = makeMe.aNote("Just say 'Yes'").creatorAndOwner(currentUser).please().getNotebook();
      saveCertificate = new SaveCertificateDetails();
      saveCertificate.setNotebook(notebook);
    }

    @Test
    void ShouldReturnCertificateForCurrentUser() {
      Certificate cert = controller.saveCertificate(saveCertificate);
      assertEquals(currentUser.getEntity(), cert.getUser());
    }

    @Test
    void ShouldReturnCertificateForNotebook() {
      Certificate cert = controller.saveCertificate(saveCertificate);
      assertEquals(notebook, cert.getNotebook());
    }
  }
}
