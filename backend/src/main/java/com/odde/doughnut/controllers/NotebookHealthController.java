package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.NotebookHealthFixRequest;
import com.odde.doughnut.controllers.dto.NotebookHealthLintReport;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.NotebookHealthService;
import com.odde.doughnut.services.health.HealthRunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/notebooks")
class NotebookHealthController {

  private final AuthorizationService authorizationService;
  private final NotebookHealthService notebookHealthService;

  NotebookHealthController(
      AuthorizationService authorizationService, NotebookHealthService notebookHealthService) {
    this.authorizationService = authorizationService;
    this.notebookHealthService = notebookHealthService;
  }

  @PostMapping("/{notebook}/health/lint")
  @Transactional(readOnly = true)
  public NotebookHealthLintReport lint(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(notebook);
    return notebookHealthService.lint(
        notebook, new HealthRunContext(authorizationService.getCurrentUser()));
  }

  @PostMapping("/{notebook}/health/fix")
  @Transactional
  public void fix(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @Valid @RequestBody NotebookHealthFixRequest request)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(notebook);
    notebookHealthService.fix(notebook, request);
  }
}
