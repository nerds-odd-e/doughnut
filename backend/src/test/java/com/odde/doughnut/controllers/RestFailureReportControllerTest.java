package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.FailureReport;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.GithubService;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.NullGithubService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RestFailureReportControllerTest {
  @Autowired MakeMe makeMe;
  private GithubService githubService = new NullGithubService();

  RestFailureReportController controller(UserModel userModel) {
    return new RestFailureReportController(makeMe.modelFactoryService, githubService, userModel);
  }

  @Test
  void whenNonAdminAccessTheFailureReport() {
    UserModel nonAdmin = makeMe.aUser().toModelPlease();
    FailureReport failureReport = makeMe.aFailureReport().please();
    assertThrows(
        UnexpectedNoAccessRightException.class, () -> controller(nonAdmin).show(failureReport));
  }
}
