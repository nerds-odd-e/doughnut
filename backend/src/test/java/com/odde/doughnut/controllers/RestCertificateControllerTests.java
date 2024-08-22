package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.odde.doughnut.entities.Certificate;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.models.TimestampOperations;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import java.sql.Timestamp;
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
  Timestamp currentTime;
  TestabilitySettings testabilitySettings = new TestabilitySettings();

  @BeforeEach
  void setup() {
    currentTime = makeMe.aTimestamp().please();
    testabilitySettings.timeTravelTo(currentTime);
    currentUser = makeMe.aUser().toModelPlease();
    controller = new RestCertificateController(currentUser, this.testabilitySettings);
  }

  // Create a nested test class for the getCertificate method
  @Nested
  class SaveCertificate {

    private Notebook notebook;

    @BeforeEach
    void setup() {
      notebook = makeMe.aNote("Just say 'Yes'").creatorAndOwner(currentUser).please().getNotebook();
    }

    @Test
    void ShouldReturnCompleteCertificateData() {
      Certificate cert = controller.saveCertificate(notebook);
      assertEquals(currentUser.getEntity(), cert.getUser());
      assertEquals(notebook, cert.getNotebook());
      assertEquals(currentTime, cert.getStartDate());
      // Set expiry date to 1 year from current time
      Timestamp expiryDate =
          TimestampOperations.addHoursToTimestamp(new Timestamp(currentTime.getTime()), 8760);
      assertEquals(expiryDate, cert.getExpiryDate());
    }
  }
}
