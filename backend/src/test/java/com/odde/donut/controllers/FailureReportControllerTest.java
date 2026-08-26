package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.FailureReport;
import com.odde.donut.entities.repositories.FailureReportRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class FailureReportControllerTest extends ControllerTestBase {
  @Autowired FailureReportRepository failureReportRepository;
  @Autowired FailureReportController controller;

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
      controller.deleteFailureReports(List.of(first.getId(), second.getId()));

      assertThat(remainingReports(), empty());
    }

    @Test
    void adminCanDeleteOneFailureReport() throws UnexpectedNoAccessRightException {
      controller.deleteFailureReports(List.of(first.getId()));

      List<FailureReport> remaining = remainingReports();
      assertThat(remaining, hasSize(1));
      assertThat(remaining.getFirst().getId(), equalTo(second.getId()));
    }

    @Test
    void nonAdminCannotDeleteFailureReports() {
      currentUser.setUser(makeMe.aUser().please());
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.deleteFailureReports(List.of(first.getId(), second.getId())));
    }

    private List<FailureReport> remainingReports() throws UnexpectedNoAccessRightException {
      return StreamSupport.stream(controller.failureReports().spliterator(), false).toList();
    }
  }
}
