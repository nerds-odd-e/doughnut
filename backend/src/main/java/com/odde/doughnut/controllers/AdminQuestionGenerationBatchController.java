package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.QuestionGenerationBatchAdminStatusDTO;
import com.odde.doughnut.controllers.dto.QuestionGenerationBatchSubmissionSummaryDTO;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.QuestionGenerationBatchAdminStatusService;
import com.odde.doughnut.services.QuestionGenerationBatchMaintenanceRunState;
import com.odde.doughnut.services.QuestionGenerationBatchMaintenanceService;
import com.odde.doughnut.services.QuestionGenerationBatchSubmitDueUsersService;
import com.odde.doughnut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.Operation;
import java.sql.Timestamp;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/question-generation-batch")
class AdminQuestionGenerationBatchController {

  private final AuthorizationService authorizationService;
  private final QuestionGenerationBatchAdminStatusService adminStatusService;
  private final QuestionGenerationBatchMaintenanceService maintenanceService;
  private final QuestionGenerationBatchMaintenanceRunState maintenanceRunState;
  private final QuestionGenerationBatchSubmitDueUsersService submitDueUsersService;
  private final TestabilitySettings testabilitySettings;

  AdminQuestionGenerationBatchController(
      AuthorizationService authorizationService,
      QuestionGenerationBatchAdminStatusService adminStatusService,
      QuestionGenerationBatchMaintenanceService maintenanceService,
      QuestionGenerationBatchMaintenanceRunState maintenanceRunState,
      QuestionGenerationBatchSubmitDueUsersService submitDueUsersService,
      TestabilitySettings testabilitySettings) {
    this.authorizationService = authorizationService;
    this.adminStatusService = adminStatusService;
    this.maintenanceService = maintenanceService;
    this.maintenanceRunState = maintenanceRunState;
    this.submitDueUsersService = submitDueUsersService;
    this.testabilitySettings = testabilitySettings;
  }

  @Operation(operationId = "getQuestionGenerationBatchStatus")
  @GetMapping("/status")
  public QuestionGenerationBatchAdminStatusDTO getQuestionGenerationBatchStatus()
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    authorizationService.assertAdminAuthorization();
    return adminStatusService.getStatus();
  }

  @Operation(operationId = "submitRecentRecallUsersForQuestionGenerationBatch")
  @PostMapping("/submit-recent-recall-users")
  public QuestionGenerationBatchSubmissionSummaryDTO submitRecentRecallUsers()
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    authorizationService.assertAdminAuthorization();
    return submitDueUsersService.submitUsersWithRecentRecalls(
        testabilitySettings.getCurrentUTCTimestamp());
  }

  @Operation(operationId = "resumeExistingQuestionGenerationBatches")
  @PostMapping("/resume-existing-batches")
  public QuestionGenerationBatchAdminStatusDTO resumeExistingBatches()
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    authorizationService.assertAdminAuthorization();
    Timestamp currentTime = testabilitySettings.getCurrentUTCTimestamp();
    maintenanceRunState.recordStarted(currentTime);
    try {
      maintenanceService.resumeExistingBatches(currentTime);
    } catch (RuntimeException e) {
      maintenanceRunState.recordError(e);
      throw e;
    } finally {
      maintenanceRunState.recordFinished(testabilitySettings.getCurrentUTCTimestamp());
    }
    return adminStatusService.getStatus();
  }
}
