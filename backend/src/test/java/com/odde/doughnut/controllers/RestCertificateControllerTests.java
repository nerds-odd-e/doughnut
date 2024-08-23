package com.odde.doughnut.controllers;

import static com.odde.doughnut.controllers.RestCertificateController.oneYearInHours;
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
    controller =
        new RestCertificateController(currentUser, testabilitySettings, makeMe.modelFactoryService);
  }

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
      Timestamp expiryDate =
          TimestampOperations.addHoursToTimestamp(
              new Timestamp(currentTime.getTime()), oneYearInHours);
      assertEquals(expiryDate, cert.getExpiryDate());
    }
  }

  @Nested
  class GetCertificate {
    private Notebook notebook;
    private Certificate expectedCertificate;

    @BeforeEach
    void setup() {
      notebook = makeMe.aNote("Just say 'Yes'").creatorAndOwner(currentUser).please().getNotebook();
      expectedCertificate = makeMe.aCertificate(notebook, currentUser, currentTime).please();
    }

    @Test
    void ReturnsACertificate() {
      Certificate c =
          new Certificate() {
            {
              id = expectedCertificate.getId();
            }
          };
      Certificate cert = controller.getCertificate(notebook);
      assertEquals(currentUser.getEntity(), cert.getUser());
      assertEquals(notebook, cert.getNotebook());
      assertEquals(currentTime, cert.getStartDate());
      Timestamp expiryDate =
          TimestampOperations.addHoursToTimestamp(
              new Timestamp(currentTime.getTime()), oneYearInHours);
      assertEquals(expiryDate, cert.getExpiryDate());
    }

    @Test
    void SaveTwiceGetOriginalStartDate() {
      Timestamp currentTimeAtStart = currentTime;
      Timestamp newStartDate =
          TimestampOperations.addHoursToTimestamp(
              new Timestamp(currentTime.getTime()), oneYearInHours);
      testabilitySettings.timeTravelTo(newStartDate);
      Certificate certLater = controller.saveCertificate(notebook);
      assertEquals(currentTimeAtStart, certLater.getStartDate());
      assertEquals(expectedCertificate.getId(), certLater.getId());
    }
  }
}
