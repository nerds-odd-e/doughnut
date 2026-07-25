package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.NotebookHealthFixRequest;
import com.odde.doughnut.controllers.dto.NotebookHealthLintReport;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.services.health.EmptyFolderBulkPurge;
import com.odde.doughnut.services.health.HealthRuleRunner;
import com.odde.doughnut.services.health.HealthRunContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotebookHealthService {
  private final HealthRuleRunner healthRuleRunner;
  private final EmptyFolderBulkPurge emptyFolderBulkPurge;

  public NotebookHealthService(
      HealthRuleRunner healthRuleRunner, EmptyFolderBulkPurge emptyFolderBulkPurge) {
    this.healthRuleRunner = healthRuleRunner;
    this.emptyFolderBulkPurge = emptyFolderBulkPurge;
  }

  public NotebookHealthLintReport lint(Notebook notebook, HealthRunContext context) {
    return healthRuleRunner.run(notebook, context);
  }

  public void fix(Notebook notebook, NotebookHealthFixRequest request) {
    if (!Boolean.TRUE.equals(request.getRemoveEmptyFolders())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Fix requires removeEmptyFolders=true");
    }
    emptyFolderBulkPurge.apply(notebook);
  }
}
