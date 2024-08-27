package com.odde.doughnut.controllers;

import static com.odde.doughnut.controllers.RestCertificateController.oneYearInHours;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.Certificate;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.ApiException;
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
import org.springframework.web.server.ResponseStatusException;

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
    void mustFromALoginUser() {
      controller =
          new RestCertificateController(
              makeMe.aNullUserModelPlease(), testabilitySettings, makeMe.modelFactoryService);
      assertThrows(ResponseStatusException.class, () -> controller.claimCertificate(notebook));
    }

    @Test
    void shouldNotAllowToClaimCertificateIfTheUserHasNotTakenTheAssessment() {
      assertThrows(ApiException.class, () -> controller.claimCertificate(notebook));
    }

    @Test
    void shouldNotAllowToClaimCertificateIfTheUserHasNotPassedTheAssessment() {
      makeMe.anAssessmentAttempt(currentUser.getEntity(), notebook).score(20, 0).please();
      assertThrows(ApiException.class, () -> controller.claimCertificate(notebook));
    }

    @Test
    void shouldNotAllowToClaimCertificateIfTheUserLastAttemptFailed() {
      makeMe.anAssessmentAttempt(currentUser.getEntity(), notebook).score(20, 20).please();
      makeMe.anAssessmentAttempt(currentUser.getEntity(), notebook).score(20, 0).please();
      assertThrows(ApiException.class, () -> controller.claimCertificate(notebook));
    }

    @Test
    void ShouldBeAbleToClaimCertificateIfLastAttemptPassed() {
      makeMe.anAssessmentAttempt(currentUser.getEntity(), notebook).score(20, 0).please();
      makeMe.anAssessmentAttempt(currentUser.getEntity(), notebook).score(20, 20).please();
      Certificate cert = controller.claimCertificate(notebook);
      assertEquals(currentUser.getEntity(), cert.getUser());
    }

    @Test
    void ShouldReturnCompleteCertificateData() {
      makeMe.anAssessmentAttempt(currentUser.getEntity(), notebook).score(20, 20).please();
      Certificate cert = controller.claimCertificate(notebook);
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
      makeMe.anAssessmentAttempt(currentUser.getEntity(), notebook).score(20, 20).please();
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
