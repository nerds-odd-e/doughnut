package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.AssimilationCountDTO;

public class AssimilationCounter {
  private final int subscribedUnitCount;
  private final int ownedUnitCount;
  private final int assimilatedToday;
  private final int dailyLimit;

  public AssimilationCounter(
      int subscribedUnitCount, int ownedUnitCount, int assimilatedToday, int dailyLimit) {
    this.subscribedUnitCount = subscribedUnitCount;
    this.ownedUnitCount = ownedUnitCount;
    this.assimilatedToday = assimilatedToday;
    this.dailyLimit = dailyLimit;
  }

  public int getTotalUnassimilated() {
    return subscribedUnitCount + ownedUnitCount;
  }

  public int getDueCount() {
    int remainingDaily = dailyLimit - assimilatedToday;
    return remainingDaily > 0 ? Math.min(remainingDaily, getTotalUnassimilated()) : 0;
  }

  public AssimilationCountDTO toDTO() {
    return new AssimilationCountDTO(getDueCount(), assimilatedToday, getTotalUnassimilated());
  }
}
