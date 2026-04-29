package com.odde.doughnut.controllers;

import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/data-migration")
class AdminDataMigrationController {

  private final AuthorizationService authorizationService;

  AdminDataMigrationController(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  @Operation(operationId = "runDataMigration", summary = "Run admin data migration (stub)")
  @PostMapping("/run")
  public ResponseEntity<Void> runDataMigration() throws UnexpectedNoAccessRightException {
    authorizationService.assertLoggedIn();
    authorizationService.assertAdminAuthorization();
    return ResponseEntity.ok().build();
  }
}
