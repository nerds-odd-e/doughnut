package com.odde.doughnut.controllers.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminDataMigrationDryRunDTO {

  private List<TitleAliasMigrationNotePreviewDTO> notePreviews = new ArrayList<>();
  private int totalNoteCount;
  private int migrateCount;
  private int noChangesCount;
}
