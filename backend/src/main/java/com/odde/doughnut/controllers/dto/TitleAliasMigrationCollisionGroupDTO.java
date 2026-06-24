package com.odde.doughnut.controllers.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TitleAliasMigrationCollisionGroupDTO {

  private int notebookId;
  private Integer folderId;
  private String basePlannedTitle;
  private List<TitleAliasMigrationCollisionMemberDTO> members = new ArrayList<>();
}
