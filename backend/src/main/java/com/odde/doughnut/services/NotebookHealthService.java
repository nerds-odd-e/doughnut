package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.NotebookHealthLintReport;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.services.health.HealthRuleRunner;
import com.odde.doughnut.services.health.HealthRunContext;
import org.springframework.stereotype.Service;

@Service
public class NotebookHealthService {
  private final HealthRuleRunner healthRuleRunner;

  public NotebookHealthService(HealthRuleRunner healthRuleRunner) {
    this.healthRuleRunner = healthRuleRunner;
  }

  public NotebookHealthLintReport lint(Notebook notebook, HealthRunContext context) {
    return healthRuleRunner.run(notebook, context);
  }
}
