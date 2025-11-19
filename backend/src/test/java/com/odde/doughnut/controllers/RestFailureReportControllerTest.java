package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.entities.FailureReport;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.GithubService;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.AuthorizationServiceTestHelper;
import com.odde.doughnut.testability.NullGithubService;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
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
class FailureReportControllerTest {
  @Autowired MakeMe makeMe;
  @Autowired AuthorizationService authorizationService;
  private GithubService githubService = new NullGithubService();

  FailureReportController controller(CurrentUser currentUser) {
    AuthorizationServiceTestHelper.setCurrentUser(authorizationService, currentUser);
    return new FailureReportController(
        makeMe.modelFactoryService, githubService, authorizationService);
  }

  @Test
  void whenNonAdminAccessTheFailureReport() {
    CurrentUser nonAdmin = new CurrentUser(makeMe.aUser().please());
    FailureReport failureReport = makeMe.aFailureReport().please();
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller(nonAdmin).showFailureReport(failureReport));
  }

  @Nested
  class DeleteFailureReportsTest {
    CurrentUser admin;
    List<FailureReport> failureReports;

    @BeforeEach
    void setup() {
      admin = new CurrentUser(makeMe.anAdmin().please());

      // Clear all existing failure reports first to ensure test independence
      makeMe.modelFactoryService.failureReportRepository.deleteAll();

      failureReports = new ArrayList<>();
      failureReports.add(makeMe.aFailureReport().please());
      failureReports.add(makeMe.aFailureReport().please());
    }

    @Test
    void adminCanDeleteFailureReports() throws UnexpectedNoAccessRightException {
      List<Integer> idsToDelete =
          failureReports.stream().map(FailureReport::getId).collect(Collectors.toList());

      controller(admin).deleteFailureReports(idsToDelete);

      Iterable<FailureReport> remainingReports = controller(admin).failureReports();
      List<FailureReport> reportList =
          StreamSupport.stream(remainingReports.spliterator(), false).collect(Collectors.toList());
      assertThat(reportList, is(empty()));
    }

    @Test
    void adminCanDeleteOneFailureReport() throws UnexpectedNoAccessRightException {
      List<Integer> idsToDelete = List.of(failureReports.get(0).getId());

      controller(admin).deleteFailureReports(idsToDelete);

      Iterable<FailureReport> remainingReports = controller(admin).failureReports();
      List<FailureReport> reportList =
          StreamSupport.stream(remainingReports.spliterator(), false).collect(Collectors.toList());
      assertThat(reportList, hasSize(1));
      assertThat(reportList.get(0).getId(), equalTo(failureReports.get(1).getId()));
    }

    @Test
    void nonAdminCannotDeleteFailureReports() {
      CurrentUser nonAdmin = new CurrentUser(makeMe.aUser().please());
      List<Integer> idsToDelete =
          failureReports.stream().map(FailureReport::getId).collect(Collectors.toList());

      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller(nonAdmin).deleteFailureReports(idsToDelete));
    }
  }
}
