package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.AdminDataMigrationDryRunDTO;
import com.odde.doughnut.controllers.dto.AdminDataMigrationStatusDTO;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AdminDataMigrationService;
import com.odde.doughnut.services.AuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/data-migration")
class AdminDataMigrationController {

  private final AuthorizationService authorizationService;
  private final AdminDataMigrationService adminDataMigrationService;

  AdminDataMigrationController(
      AuthorizationService authorizationService,
      AdminDataMigrationService adminDataMigrationService) {
    this.authorizationService = authorizationService;
    this.adminDataMigrationService = adminDataMigrationService;
  }

  @Operation(
      operationId = "getAdminDataMigrationStatus",
      summary = "Get admin data migration status")
  @GetMapping("/status")
  public AdminDataMigrationStatusDTO getAdminDataMigrationStatus()
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    authorizationService.assertAdminAuthorization();
    return adminDataMigrationService.getStatus();
  }

  @Operation(operationId = "runDataMigrationBatch")
  @PostMapping("/run-batch")
  public AdminDataMigrationStatusDTO runDataMigrationBatch()
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    authorizationService.assertAdminAuthorization();
    User admin = authorizationService.getCurrentUser();
    return adminDataMigrationService.runBatch(admin);
  }

  @Operation(
      operationId = "getAdminDataMigrationDryRun",
      summary = "Preview title-alias to frontmatter migration without mutating notes")
  @GetMapping("/dry-run")
  public AdminDataMigrationDryRunDTO getAdminDataMigrationDryRun()
      throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    authorizationService.assertAdminAuthorization();
    return adminDataMigrationService.dryRun();
  }
}
