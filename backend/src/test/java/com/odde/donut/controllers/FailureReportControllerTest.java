package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.odde.donut.controllers.dto.FailureReportDeletionResultDTO;
import com.odde.donut.entities.FailureReport;
import com.odde.donut.entities.repositories.FailureReportRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.GithubService;
import java.io.IOException;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class FailureReportControllerTest extends ControllerTestBase {
  @Autowired FailureReportRepository failureReportRepository;
  @Autowired FailureReportController controller;
  @MockitoBean GithubService githubService;

  @Test
  void nonAdminCannotShowFailureReport() {
    currentUser.setUser(makeMe.aUser().please());
    FailureReport failureReport = makeMe.aFailureReport().please();
    assertThrows(
        UnexpectedNoAccessRightException.class, () -> controller.showFailureReport(failureReport));
  }

  @Nested
  class TriggerException {
    @Test
    void adminCanTriggerException() {
      currentUser.setUser(makeMe.anAdmin().please());
      assertThrows(RuntimeException.class, () -> controller.triggerFailure());
    }

    @Test
    void nonAdminCannotTriggerException() {
      currentUser.setUser(makeMe.aUser().please());
      assertThrows(UnexpectedNoAccessRightException.class, () -> controller.triggerFailure());
    }
  }

  @Nested
  class DeleteFailureReports {
    FailureReport first;
    FailureReport second;

    @BeforeEach
    void setup() {
      currentUser.setUser(makeMe.anAdmin().please());
      failureReportRepository.deleteAll();
      first = makeMe.aFailureReport().please();
      second = makeMe.aFailureReport().please();
    }

    @Test
    void adminCanDeleteAllListedReports()
        throws UnexpectedNoAccessRightException, IOException, InterruptedException {
      FailureReportDeletionResultDTO result =
          controller.deleteFailureReports(List.of(first.getId(), second.getId()));

      assertThat(remainingReports(), empty());
      assertThat(result.getUnresolvedGithubIssueUrls(), empty());
    }

    @Test
    void adminCanDeleteOneFailureReport()
        throws UnexpectedNoAccessRightException, IOException, InterruptedException {
      FailureReportDeletionResultDTO result =
          controller.deleteFailureReports(List.of(first.getId()));

      List<FailureReport> remaining = remainingReports();
      assertThat(remaining, hasSize(1));
      assertThat(remaining.getFirst().getId(), equalTo(second.getId()));
      assertThat(result.getUnresolvedGithubIssueUrls(), empty());
    }

    @Test
    void nonAdminCannotDeleteFailureReports() {
      currentUser.setUser(makeMe.aUser().please());
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.deleteFailureReports(List.of(first.getId(), second.getId())));
      verifyNoInteractions(githubService);
    }

    @Test
    void deletingReportWithLinkedIssueResolvesTheGithubIssue()
        throws UnexpectedNoAccessRightException, IOException, InterruptedException {
      FailureReport withIssue = makeMe.aFailureReport().withIssueNumber(42).please();

      controller.deleteFailureReports(List.of(withIssue.getId()));

      verify(githubService).closeIssueAsCompleted(42);
      assertThat(failureReportRepository.findById(withIssue.getId()).isPresent(), equalTo(false));
    }

    @Test
    void deletingReportWithoutIssueNumberSkipsGithubCall()
        throws UnexpectedNoAccessRightException, IOException, InterruptedException {
      controller.deleteFailureReports(List.of(first.getId()));

      verify(githubService, never()).closeIssueAsCompleted(any());
      assertThat(failureReportRepository.findById(first.getId()).isPresent(), equalTo(false));
    }

    @Test
    void unselectedReportIsRetainedAndItsIssueUntouched()
        throws UnexpectedNoAccessRightException, IOException, InterruptedException {
      FailureReport untouched = makeMe.aFailureReport().withIssueNumber(99).please();

      controller.deleteFailureReports(List.of(first.getId()));

      assertThat(failureReportRepository.findById(untouched.getId()).isPresent(), equalTo(true));
      verify(githubService, never()).closeIssueAsCompleted(99);
    }

    private List<FailureReport> remainingReports() throws UnexpectedNoAccessRightException {
      return StreamSupport.stream(controller.failureReports().spliterator(), false).toList();
    }
  }
}
