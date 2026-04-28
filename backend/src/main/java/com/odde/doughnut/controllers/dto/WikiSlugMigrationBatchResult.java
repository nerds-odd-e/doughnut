package com.odde.doughnut.controllers.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WikiSlugMigrationBatchResult {
  private long processedInBatch;
  private WikiSlugMigrationStatus status;
}
