package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.AssimilationCountDTO;

public class AssimilationCounter {
    private final int subscribedCount;
    private final int userCount;
    private final int assimilatedToday;
    private final int dailyLimit;

    public AssimilationCounter(int subscribedCount, int userCount, int assimilatedToday, int dailyLimit) {
        this.subscribedCount = subscribedCount;
        this.userCount = userCount;
        this.assimilatedToday = assimilatedToday;
        this.dailyLimit = dailyLimit;
    }

    public int getTotalUnassimilated() {
        return subscribedCount + userCount;
    }

    public int getDueCount() {
        int remainingDaily = dailyLimit - assimilatedToday;
        return remainingDaily > 0 ? Math.min(remainingDaily, getTotalUnassimilated()) : 0;
    }

    public AssimilationCountDTO toDTO() {
        return new AssimilationCountDTO(
            getDueCount(),
            assimilatedToday,
            getTotalUnassimilated()
        );
    }
} 
