package com.odde.donut.controllers;

import com.odde.donut.controllers.dto.QuestionGenerationBatchAdminStatusDTO;
import com.odde.donut.controllers.dto.QuestionGenerationBatchSubmissionSummaryDTO;
import com.odde.donut.entities.QuestionGenerationBatchMaintenanceTriggerSource;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.QuestionGenerationBatchAdminStatusService;
import com.odde.donut.services.QuestionGenerationBatchMaintenanceRunService;
import com.odde.donut.services.QuestionGenerationBatchMaintenanceService;
import com.odde.donut.services.QuestionGenerationBatchSubmitDueUsersService;
import com.odde.donut.testability.TestabilitySettings;
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
  private final QuestionGenerationBatchMaintenanceRunService maintenanceRunService;
  private final QuestionGenerationBatchSubmitDueUsersService submitDueUsersService;
  private final TestabilitySettings testabilitySettings;

  AdminQuestionGenerationBatchController(
      AuthorizationService authorizationService,
      QuestionGenerationBatchAdminStatusService adminStatusService,
      QuestionGenerationBatchMaintenanceService maintenanceService,
      QuestionGenerationBatchMaintenanceRunService maintenanceRunService,
      QuestionGenerationBatchSubmitDueUsersService submitDueUsersService,
      TestabilitySettings testabilitySettings) {
    this.authorizationService = authorizationService;
    this.adminStatusService = adminStatusService;
    this.maintenanceService = maintenanceService;
    this.maintenanceRunService = maintenanceRunService;
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
    maintenanceRunService.recordStarted(
        QuestionGenerationBatchMaintenanceTriggerSource.MANUAL_RESUME, currentTime);
    try {
      maintenanceService.resumeExistingBatches(currentTime);
    } catch (RuntimeException e) {
      maintenanceRunService.recordError(e);
      throw e;
    } finally {
      maintenanceRunService.recordFinished(testabilitySettings.getCurrentUTCTimestamp());
    }
    return adminStatusService.getStatus();
  }
}
