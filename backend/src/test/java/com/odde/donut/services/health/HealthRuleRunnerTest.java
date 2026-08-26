package com.odde.donut.services.health;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

import com.odde.donut.controllers.dto.NotebookHealthLintReport;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
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
