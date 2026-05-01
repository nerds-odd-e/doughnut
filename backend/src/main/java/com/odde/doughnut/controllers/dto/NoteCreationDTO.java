package com.odde.doughnut.controllers.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

public class NoteCreationDTO extends NoteUpdateTitleDTO {
  @Getter
  @Setter
  @Pattern(regexp = "^$|Q\\d+", message = "The wikidata Id should be Q<numbers>")
  public String wikidataId;

  @Getter
  @Setter
  @Size(max = 500)
  private String description;

  @Getter @Setter private Integer folderId;
}
