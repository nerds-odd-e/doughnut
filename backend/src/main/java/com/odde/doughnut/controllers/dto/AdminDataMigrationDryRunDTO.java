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
  private List<TitleAliasMigrationCollisionGroupDTO> collisionGroups = new ArrayList<>();
  private int collisionGroupCount;
  private int collisionNoteCount;
  private List<TitleAliasInboundReferenceRewritePreviewDTO> inboundReferenceRewritePreviews =
      new ArrayList<>();
  private int inboundReferenceRewriteCount;
}
