package com.odde.donut.testability.builders;

import com.odde.donut.entities.DailyProbe;
import com.odde.donut.entities.User;
import com.odde.donut.testability.EntityBuilder;
import com.odde.donut.testability.MakeMe;

public class DailyProbeBuilder extends EntityBuilder<DailyProbe> {

  public DailyProbeBuilder(MakeMe makeMe) {
    super(makeMe, new DailyProbe());
    entity.setCompletedAt(makeMe.testabilitySettings.getCurrentUTCTimestamp());
    entity.setSpeed(4.0);
    entity.setAccuracy(100);
    entity.setLapseCount(0);
    entity.setVariability(0.0);
    entity.setTrialsJson(completedTrialsJson());
  }

  public DailyProbeBuilder by(User user) {
    entity.setUser(user);
    return this;
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (entity.getUser() == null) {
      entity.setUser(makeMe.aUser().please(needPersist));
    }
  }

  private static String completedTrialsJson() {
    StringBuilder json = new StringBuilder("[");
    for (int i = 0; i < 20; i++) {
      if (i > 0) {
        json.append(',');
      }
      json.append("{\"stimulus\":\"left\",\"response\":\"left\",\"rtMs\":250,\"correct\":true}");
    }
    json.append(']');
    return json.toString();
  }
}
