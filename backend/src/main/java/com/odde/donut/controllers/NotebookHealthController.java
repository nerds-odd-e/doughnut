package com.odde.donut.controllers;

import com.odde.donut.controllers.dto.NotebookHealthFixRequest;
import com.odde.donut.controllers.dto.NotebookHealthLintReport;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.NotebookHealthService;
import com.odde.donut.services.health.HealthRunContext;
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
