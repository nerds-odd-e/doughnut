package com.odde.doughnut.controllers.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TitleAliasMigrationNotePreviewDTO {

  private int noteId;
  private String currentTitle;
  private String plannedTitle;
  private List<String> plannedAliases = new ArrayList<>();
  private String plannedContent;

  /** Values: NO_CHANGES, MIGRATE (see TitleAliasMigrationPreviewStatus). */
  private String status;
}
