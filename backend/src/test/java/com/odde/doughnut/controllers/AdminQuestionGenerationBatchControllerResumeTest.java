package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odde.doughnut.controllers.dto.QuestionGenerationBatchAdminStatusDTO;
import com.odde.doughnut.entities.QuestionGenerationBatchMaintenanceTriggerSource;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.QuestionGenerationBatchAdminStatusService;
import com.odde.doughnut.services.QuestionGenerationBatchMaintenanceRunService;
import com.odde.doughnut.services.QuestionGenerationBatchMaintenanceService;
import com.odde.doughnut.services.QuestionGenerationBatchSubmitDueUsersService;
import com.odde.doughnut.testability.TestabilitySettings;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class AdminQuestionGenerationBatchControllerResumeTest {

  AuthorizationService authorizationService;
  QuestionGenerationBatchAdminStatusService adminStatusService;
  QuestionGenerationBatchMaintenanceService maintenanceService;
  QuestionGenerationBatchMaintenanceRunService maintenanceRunService;
  QuestionGenerationBatchSubmitDueUsersService submitDueUsersService;
  TestabilitySettings testabilitySettings;
  AdminQuestionGenerationBatchController controller;
  Timestamp currentTime;

  @BeforeEach
  void setup() throws UnexpectedNoAccessRightException {
    authorizationService = mock(AuthorizationService.class);
    adminStatusService = mock(QuestionGenerationBatchAdminStatusService.class);
    maintenanceService = mock(QuestionGenerationBatchMaintenanceService.class);
    maintenanceRunService = mock(QuestionGenerationBatchMaintenanceRunService.class);
    submitDueUsersService = mock(QuestionGenerationBatchSubmitDueUsersService.class);
    testabilitySettings = mock(TestabilitySettings.class);
    controller =
        new AdminQuestionGenerationBatchController(
            authorizationService,
            adminStatusService,
            maintenanceService,
            maintenanceRunService,
            submitDueUsersService,
            testabilitySettings);
    currentTime = Timestamp.valueOf("2026-07-10 08:00:00");
    doNothing().when(authorizationService).assertLoggedIn();
    doNothing().when(authorizationService).assertAdminAuthorization();
    when(testabilitySettings.getCurrentUTCTimestamp()).thenReturn(currentTime);
  }

  @Test
  void resumeExistingBatchesRecordsManualRunAndReturnsRefreshedStatus()
      throws UnexpectedNoAccessRightException {
    QuestionGenerationBatchAdminStatusDTO status = new QuestionGenerationBatchAdminStatusDTO();
    when(adminStatusService.getStatus()).thenReturn(status);

    QuestionGenerationBatchAdminStatusDTO result = controller.resumeExistingBatches();

    InOrder inOrder = inOrder(maintenanceRunService, maintenanceService, adminStatusService);
    inOrder
        .verify(maintenanceRunService)
        .recordStarted(QuestionGenerationBatchMaintenanceTriggerSource.MANUAL_RESUME, currentTime);
    inOrder.verify(maintenanceService).resumeExistingBatches(currentTime);
    inOrder.verify(maintenanceRunService).recordFinished(currentTime);
    inOrder.verify(adminStatusService).getStatus();
    assertThat(result, sameInstance(status));
  }

  @Test
  void resumeExistingBatchesRecordsErrorAndRethrowsWhenMaintenanceFails()
      throws UnexpectedNoAccessRightException {
    RuntimeException failure = new RuntimeException("poll failed");
    doThrow(failure).when(maintenanceService).resumeExistingBatches(currentTime);

    assertThrows(RuntimeException.class, () -> controller.resumeExistingBatches());

    verify(maintenanceRunService)
        .recordStarted(QuestionGenerationBatchMaintenanceTriggerSource.MANUAL_RESUME, currentTime);
    verify(maintenanceRunService).recordError(failure);
    verify(maintenanceRunService).recordFinished(currentTime);
  }
}
