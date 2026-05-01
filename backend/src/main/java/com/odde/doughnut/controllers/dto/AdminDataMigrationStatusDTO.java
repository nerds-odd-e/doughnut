package com.odde.doughnut.controllers.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminDataMigrationStatusDTO {

  /** Human-readable migration status placeholder for admin UI. */
  private String message;

  private boolean dataMigrationComplete;

  /** Capability-named step, e.g. relationship_title_backfill. */
  private String currentStepName;

  /** Values: PENDING, RUNNING, COMPLETED, FAILED (see WikiReferenceMigrationStepStatus). */
  private String stepStatus;

  private int processedCount;
  private int totalCount;
  private String lastError;
}
