package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.odde.doughnut.entities.Certificate;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.TimestampService;
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
public class CertificateControllerTests {
  public static final int oneYearInHours = 8760;
  @Autowired MakeMe makeMe;
  @Autowired TimestampService timestampService;
  private UserModel currentUser;
  private CertificateController controller;
  Timestamp currentTime;
  TestabilitySettings testabilitySettings = new TestabilitySettings();

  @BeforeEach
  void setup() {
    currentTime = makeMe.aTimestamp().please();
    testabilitySettings.timeTravelTo(currentTime);
    currentUser = makeMe.aUser().toModelPlease();
    controller = new CertificateController(currentUser, makeMe.modelFactoryService);
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
          timestampService.addHoursToTimestamp(
              new Timestamp(currentTime.getTime()), oneYearInHours);
      assertEquals(expiryDate, cert.getExpiryDate());
    }

    @Test
    void SaveTwiceGetOriginalStartDate() {
      makeMe.anAssessmentAttempt(currentUser.getEntity()).notebook(notebook).score(20, 20).please();
      Timestamp currentTimeAtStart = currentTime;
      Timestamp newStartDate =
          timestampService.addHoursToTimestamp(
              new Timestamp(currentTime.getTime()), oneYearInHours);
      testabilitySettings.timeTravelTo(newStartDate);
      Certificate certLater = controller.claimCertificate(notebook);
      assertEquals(currentTimeAtStart, certLater.getStartDate());
      assertEquals(expectedCertificate.getId(), certLater.getId());
    }
  }
}
