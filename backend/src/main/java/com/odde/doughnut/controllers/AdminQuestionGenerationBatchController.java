package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.QuestionGenerationBatchAdminStatusDTO;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.QuestionGenerationBatchAdminStatusService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/question-generation-batch")
class AdminQuestionGenerationBatchController {

  private final AuthorizationService authorizationService;
  private final QuestionGenerationBatchAdminStatusService adminStatusService;

  AdminQuestionGenerationBatchController(
      AuthorizationService authorizationService,
      QuestionGenerationBatchAdminStatusService adminStatusService) {
    this.authorizationService = authorizationService;
    this.adminStatusService = adminStatusService;
  }

  @Operation(operationId = "getQuestionGenerationBatchStatus")
  @GetMapping("/status")
  public QuestionGenerationBatchAdminStatusDTO getQuestionGenerationBatchStatus()
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    authorizationService.assertAdminAuthorization();
    return adminStatusService.getStatus();
  }
}
