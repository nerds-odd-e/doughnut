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
}
