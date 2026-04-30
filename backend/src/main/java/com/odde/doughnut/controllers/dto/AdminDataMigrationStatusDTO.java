package com.odde.doughnut.controllers.dto;

import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminDataMigrationStatusDTO {

  /** Whether migration has finished at least once successfully in this process. */
  private boolean completedOnce;

  private Instant lastCompletedAt;

  /** Human-readable outcome for the UI. */
  private String message;

  private int detachedChildFoldersFromIndexFolder;

  private int updatedNormalNotesDetachedFromIndex;

  private int updatedRelationNotesClearedFolder;

  private int deletedObsoleteNotebookNameRootFolders;

  private long notebookCountSlugScan;

  /** True while a migration run has started and not reached the slug-regen terminal state yet. */
  private boolean migrationInProgress;

  /**
   * When true, callers should POST another migration batch shortly. Cleared once the slug phase is
   * fully applied for every notebook or when no session is active.
   */
  private boolean moreBatchesRemain;

  /** Completed HTTP-sized batches during the current migration run (0 when idle). */
  private int completedBatchOrdinal;

  /** Expected batch count for a full migration in this dataset (topology + prep + notebooks). */
  private int batchTotalPlanned;

  /** Short phrase for batched migration progress bars. */
  private String batchPhaseSummary;
}
