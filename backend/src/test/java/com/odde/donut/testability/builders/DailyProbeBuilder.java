package com.odde.donut.testability.builders;

import com.odde.donut.controllers.dto.DailyProbeRequestDTO;
import com.odde.donut.controllers.dto.DailyProbeTrialDTO;
import com.odde.donut.entities.DailyProbe;
import com.odde.donut.entities.User;
import com.odde.donut.testability.EntityBuilder;
import com.odde.donut.testability.MakeMe;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class DailyProbeBuilder extends EntityBuilder<DailyProbe> {

  public DailyProbeBuilder(MakeMe makeMe) {
    super(makeMe, new DailyProbe());
    entity.setCompletedAt(makeMe.testabilitySettings.getCurrentUTCTimestamp());
    entity.setSpeed(4.0);
    entity.setAccuracy(100);
    entity.setLapseCount(0);
    entity.setVariability(0.0);
    entity.setTrials(scoredTrials());
  }

  public DailyProbeBuilder by(User user) {
    entity.setUser(user);
    return this;
  }

  public DailyProbeBuilder completedAt(Timestamp completedAt) {
    entity.setCompletedAt(completedAt);
    return this;
  }

  public DailyProbeBuilder speed(Double speed) {
    entity.setSpeed(speed);
    return this;
  }

  public DailyProbeBuilder lapseCount(int lapseCount) {
    entity.setLapseCount(lapseCount);
    return this;
  }

  public DailyProbeBuilder variability(Double variability) {
    entity.setVariability(variability);
    return this;
  }

  public DailyProbeRequestDTO createRequest() {
    DailyProbeRequestDTO request = new DailyProbeRequestDTO();
    request.trials = scoredTrials();
    request.speed = entity.getSpeed();
    request.accuracy = entity.getAccuracy();
    request.lapseCount = entity.getLapseCount();
    request.variability = entity.getVariability();
    return request;
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (entity.getUser() == null) {
      entity.setUser(makeMe.aUser().please(needPersist));
    }
  }

  private static List<DailyProbeTrialDTO> scoredTrials() {
    List<DailyProbeTrialDTO> trials = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      DailyProbeTrialDTO trial = new DailyProbeTrialDTO();
      trial.stimulus = "left";
      trial.response = "left";
      trial.rtMs = 250;
      trial.correct = true;
      trials.add(trial);
    }
    return trials;
  }
}
