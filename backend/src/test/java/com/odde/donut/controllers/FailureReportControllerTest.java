package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
    void adminCanDeleteAllListedReports() throws UnexpectedNoAccessRightException {
      FailureReportDeletionResultDTO result =
          controller.deleteFailureReports(List.of(first.getId(), second.getId()));

      assertThat(remainingReports(), empty());
      assertThat(result.getUnresolvedGithubIssueUrls(), empty());
    }

    @Test
    void adminCanDeleteOneFailureReport() throws UnexpectedNoAccessRightException {
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

    @Test
    void closureFailureForOneReportDoesNotStopDeletionOrLaterClosure()
        throws UnexpectedNoAccessRightException, IOException, InterruptedException {
      FailureReport failing = makeMe.aFailureReport().withIssueNumber(42).please();
      FailureReport succeeding = makeMe.aFailureReport().withIssueNumber(99).please();
      doThrow(new IOException("boom")).when(githubService).closeIssueAsCompleted(42);
      doNothing().when(githubService).closeIssueAsCompleted(99);
      when(githubService.getIssueUrl(42)).thenReturn("https://github.com/example/issues/42");
      when(githubService.getIssueUrl(99)).thenReturn("https://github.com/example/issues/99");

      FailureReportDeletionResultDTO result =
          controller.deleteFailureReports(List.of(failing.getId(), succeeding.getId()));

      assertThat(failureReportRepository.findById(failing.getId()).isPresent(), equalTo(false));
      assertThat(failureReportRepository.findById(succeeding.getId()).isPresent(), equalTo(false));
      verify(githubService).closeIssueAsCompleted(42);
      verify(githubService).closeIssueAsCompleted(99);
      assertThat(
          result.getUnresolvedGithubIssueUrls(), contains("https://github.com/example/issues/42"));
    }

    @Test
    void interruptedClosureStillDeletesAndRestoresInterruptFlag()
        throws UnexpectedNoAccessRightException, IOException, InterruptedException {
      FailureReport withIssue = makeMe.aFailureReport().withIssueNumber(42).please();
      doThrow(new InterruptedException("boom")).when(githubService).closeIssueAsCompleted(42);
      when(githubService.getIssueUrl(42)).thenReturn("https://github.com/example/issues/42");

      FailureReportDeletionResultDTO result =
          controller.deleteFailureReports(List.of(withIssue.getId()));

      assertThat(failureReportRepository.findById(withIssue.getId()).isPresent(), equalTo(false));
      assertThat(
          result.getUnresolvedGithubIssueUrls(), contains("https://github.com/example/issues/42"));
      assertThat(Thread.interrupted(), equalTo(true));
    }

    private List<FailureReport> remainingReports() throws UnexpectedNoAccessRightException {
      return StreamSupport.stream(controller.failureReports().spliterator(), false).toList();
    }
  }
}
