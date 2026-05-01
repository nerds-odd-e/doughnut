package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.AdminDataMigrationStatusDTO;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AdminDataMigrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AdminDataMigrationControllerTest extends ControllerTestBase {

  @Autowired AdminDataMigrationController controller;

  @Test
  void adminGetsMigrationStatus() throws UnexpectedNoAccessRightException {
    currentUser.setUser(makeMe.anAdmin().please());

    AdminDataMigrationStatusDTO status = controller.getAdminDataMigrationStatus();

    assertThat(status.getMessage(), equalTo(AdminDataMigrationService.READY_MESSAGE));
    assertThat(status.isDataMigrationComplete(), equalTo(true));
  }

  @Test
  void adminRunBatchReturnsBatchSummary() throws UnexpectedNoAccessRightException {
    currentUser.setUser(makeMe.anAdmin().please());

    AdminDataMigrationStatusDTO run = controller.runDataMigrationBatch();

    assertThat(run.getMessage(), notNullValue());
    assertThat(run.getMessage(), containsString("Batch acknowledged"));
    assertThat(
        controller.getAdminDataMigrationStatus().getMessage(),
        equalTo(AdminDataMigrationService.READY_MESSAGE));
  }

  @Test
  void nonAdminCannotGetStatus() {
    currentUser.setUser(makeMe.aUser().please());

    assertThrows(
        UnexpectedNoAccessRightException.class, () -> controller.getAdminDataMigrationStatus());
  }

  @Test
  void nonAdminCannotRunMigrationBatch() {
    currentUser.setUser(makeMe.aUser().please());

    assertThrows(UnexpectedNoAccessRightException.class, () -> controller.runDataMigrationBatch());
  }
}
