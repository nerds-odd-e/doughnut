package com.odde.donut.services.health;

import com.odde.donut.controllers.dto.NotebookHealthLintReport;
import com.odde.donut.entities.Notebook;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class HealthRuleRunner {
  private final List<HealthRule> rules;

  public HealthRuleRunner(List<HealthRule> rules) {
    this.rules = List.copyOf(rules);
  }

  public NotebookHealthLintReport run(Notebook notebook, HealthRunContext context) {
    NotebookHealthLintReport report = new NotebookHealthLintReport();
    report.setGroups(rules.stream().map(rule -> rule.evaluate(notebook, context)).toList());
    return report;
  }
}
