package com.odde.doughnut.services.health;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

import com.odde.doughnut.controllers.dto.NotebookHealthLintReport;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import java.util.List;
import org.junit.jupiter.api.Test;

class HealthRuleRunnerTest {

  @Test
  void returnsEmptyGroupsWhenNoRulesRegistered() {
    HealthRuleRunner runner = new HealthRuleRunner(List.of());
    NotebookHealthLintReport report = runner.run(new Notebook(), new HealthRunContext(new User()));

    assertThat(report.getGroups(), empty());
  }
}
