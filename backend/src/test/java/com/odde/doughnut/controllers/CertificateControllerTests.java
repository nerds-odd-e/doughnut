package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.entities.Certificate;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import com.odde.doughnut.utils.TimestampOperations;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.TestBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class CertificateControllerTests {
  public static final int oneYearInHours = 8760;
  @Autowired MakeMe makeMe;
  @Autowired AuthorizationService authorizationService;
  @TestBean private CurrentUser currentUser = new CurrentUser(null);
  private CertificateController controller;
  Timestamp currentTime;
  TestabilitySettings testabilitySettings = new TestabilitySettings();

  @BeforeEach
  void setup() {
    currentTime = makeMe.aTimestamp().please();
    testabilitySettings.timeTravelTo(currentTime);
    currentUser.setUser(makeMe.aUser().please());
    controller = new CertificateController(makeMe.modelFactoryService, authorizationService);
  }

  @Nested
  class GetCertificate {
    private Notebook notebook;
    private Certificate expectedCertificate;

    @BeforeEach
    void setup() {
      notebook =
          makeMe
              .aNote("Just say 'Yes'")
              .creatorAndOwner(currentUser.getUser())
              .please()
              .getNotebook();
      expectedCertificate =
          makeMe.aCertificate(notebook, currentUser.getUser(), currentTime).please();
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
      assertEquals(currentUser.getUser(), cert.getUser());
      assertEquals(notebook, cert.getNotebook());
      assertEquals(currentTime, cert.getStartDate());
      Timestamp expiryDate =
          TimestampOperations.addHoursToTimestamp(
              new Timestamp(currentTime.getTime()), oneYearInHours);
      assertEquals(expiryDate, cert.getExpiryDate());
    }

    @Test
    void SaveTwiceGetOriginalStartDate() {
      makeMe.anAssessmentAttempt(currentUser.getUser()).notebook(notebook).score(20, 20).please();
      Timestamp currentTimeAtStart = currentTime;
      Timestamp newStartDate =
          TimestampOperations.addHoursToTimestamp(
              new Timestamp(currentTime.getTime()), oneYearInHours);
      testabilitySettings.timeTravelTo(newStartDate);
      Certificate certLater = controller.claimCertificate(notebook);
      assertEquals(currentTimeAtStart, certLater.getStartDate());
      assertEquals(expectedCertificate.getId(), certLater.getId());
    }
  }
}
