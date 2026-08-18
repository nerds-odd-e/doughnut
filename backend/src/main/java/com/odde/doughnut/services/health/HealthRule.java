package com.odde.doughnut.services.health;

import com.odde.doughnut.controllers.dto.HealthFindingGroup;
import com.odde.doughnut.controllers.dto.HealthSeverity;
import com.odde.doughnut.entities.Notebook;

public interface HealthRule {
  String id();

  String title();

  HealthSeverity severity();

  boolean autoFixable();

  HealthFindingGroup evaluate(Notebook notebook, HealthRunContext context);

  default HealthFindingGroup findingGroup() {
    HealthFindingGroup group = new HealthFindingGroup();
    group.setRuleId(id());
    group.setTitle(title());
    group.setSeverity(severity());
    group.setAutoFixable(autoFixable());
    return group;
  }
}
