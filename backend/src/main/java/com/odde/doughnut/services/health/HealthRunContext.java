package com.odde.doughnut.services.health;

import com.odde.doughnut.entities.User;

public final class HealthRunContext {
  private final User viewer;

  public HealthRunContext(User viewer) {
    this.viewer = viewer;
  }

  public User viewer() {
    return viewer;
  }
}
