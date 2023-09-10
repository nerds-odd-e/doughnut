package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.FailureReport;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.GithubService;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.NullGithubService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
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
