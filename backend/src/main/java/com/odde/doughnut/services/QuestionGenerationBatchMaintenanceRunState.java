package com.odde.doughnut.services;

import java.sql.Timestamp;
import org.springframework.stereotype.Service;

@Service
public class QuestionGenerationBatchMaintenanceRunState {
  private Timestamp lastMaintenanceStartedAt;
  private Timestamp lastMaintenanceFinishedAt;
  private String lastMaintenanceError;

  public synchronized void recordStarted(Timestamp startedAt) {
    lastMaintenanceStartedAt = startedAt;
    lastMaintenanceError = null;
  }

  public synchronized void recordFinished(Timestamp finishedAt) {
    lastMaintenanceFinishedAt = finishedAt;
  }

  public synchronized void recordError(RuntimeException e) {
    lastMaintenanceError = e.getMessage();
  }

  public synchronized Timestamp getLastMaintenanceStartedAt() {
    return lastMaintenanceStartedAt;
  }

  public synchronized Timestamp getLastMaintenanceFinishedAt() {
    return lastMaintenanceFinishedAt;
  }

  public synchronized String getLastMaintenanceError() {
    return lastMaintenanceError;
  }
}
