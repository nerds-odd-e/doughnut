package com.odde.donut.services.health;

import com.odde.donut.entities.User;

public final class HealthRunContext {
  private final User viewer;

  public HealthRunContext(User viewer) {
    this.viewer = viewer;
  }

  public User viewer() {
    return viewer;
  }
}
